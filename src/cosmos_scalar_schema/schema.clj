(ns cosmos-scalar-schema.schema
  (:require [clojure.tools.logging :as log]
            [clojure.java.io :as cio]
            [cheshire.core :as cheshire])
  (:import (com.azure.cosmos CosmosClient
                             CosmosClientBuilder
                             ConsistencyLevel)
           (com.azure.cosmos.models CosmosContainerProperties
                                    CosmosStoredProcedureProperties
                                    CosmosStoredProcedureRequestOptions
                                    ExcludedPath
                                    IncludedPath
                                    IndexingPolicy
                                    ThroughputProperties)
           (com.scalar.db.storage.cosmos TableMetadata)))

(def ^:const ^String METADATA_DATABASE "scalardb")
(def ^:const ^String METADATA_CONTAINER "metadata")
(def ^:const ^String METADATA_PARTITION_KEY "/id")

(def ^:const ^String CONTAINER_PARTITION_KEY "/concatenatedPartitionKey")
(def ^:const ^String PARTITION_KEY_PATH "/concatenatedPartitionKey/?")
(def ^:const ^String CLUSTERING_KEY_PATH "/clusteringKey/*")

(def ^:const REGISTERED_STORED_PROCEDURE "mutate.js")

(def COORDINATOR_SCHEMA {:database "coordinator"
                         :table "state"
                         :partition-key #{"tx_id"}
                         :clustering-key #{}
                         :columns {"tx_id" "text"
                                   "tx_state" "int"
                                   "tx_created_at" "bigint"}})

(def TRANSACTION_METADATA_COLUMNS {"tx_committed_at" "bigint"
                                   "tx_id" "text"
                                   "tx_prepared_at" "bigint"
                                   "tx_state" "int"
                                   "tx_version" "int"})

(defn- get-fullname
  [database container]
  (str database "." container))

(defn get-client
  [uri password]
  (.buildClient (doto (CosmosClientBuilder.)
                  (.endpoint uri)
                  (.key password)
                  (.consistencyLevel ConsistencyLevel/STRONG)
                  .directMode)))

(defn- database-exists?
  [client database]
  (try
    (-> (.getDatabase client database) .read nil? not)
    (catch Exception _ false)))

(defn- container-exists?
  [client database container]
  (try
    (-> (.getDatabase client database)
        (.getContainer container)
        .read nil? not)
    (catch Exception _ false)))

(defn create-database
  [client database]
  (.createDatabaseIfNotExists client database))

(defn- make-container-properties
  [container]
  (if (= container METADATA_CONTAINER)
    (CosmosContainerProperties. container METADATA_PARTITION_KEY)
    (let [policy (doto (IndexingPolicy.)
                   (.setIncludedPaths
                    [(IncludedPath. PARTITION_KEY_PATH)
                     (IncludedPath. CLUSTERING_KEY_PATH)])
                   (.setExcludedPaths [(ExcludedPath. "/*")]))]
      (doto (CosmosContainerProperties. container CONTAINER_PARTITION_KEY)
        (.setIndexingPolicy policy)))))

(defn create-container
  [client database container ru]
  (let [prop (make-container-properties container)
        throughput-prop (ThroughputProperties/createManualThroughput ru)]
    (-> (.getDatabase client database)
        (.createContainerIfNotExists prop throughput-prop))))

(defn create-metadata
  [client schema]
  (when-not (database-exists? client METADATA_DATABASE)
    (create-database client METADATA_DATABASE))
  (when-not (container-exists? client METADATA_DATABASE METADATA_CONTAINER)
    (create-container client METADATA_DATABASE METADATA_CONTAINER 400))
  (let [metadata (doto (TableMetadata.)
                   (.setId (get-fullname (:database schema) (:table schema)))
                   (.setPartitionKeyNames (:partition-key schema))
                   (.setClusteringKeyNames (:clustering-key schema))
                   (.setColumns (:columns schema)))]
    (-> (.getDatabase client METADATA_DATABASE)
        (.getContainer METADATA_CONTAINER)
        (.upsertItem metadata))))

(defn register-stored-procedure
  [client database container]
  (let [scripts (-> client (.getDatabase database) (.getContainer container)
                    .getScripts)
        properties (CosmosStoredProcedureProperties.
                     REGISTERED_STORED_PROCEDURE
                     (slurp (cio/resource REGISTERED_STORED_PROCEDURE)))]
    (.createStoredProcedure scripts properties
                            (CosmosStoredProcedureRequestOptions.))))

(defn create-table
  [client schema {:keys [ru] :or {ru 400}}]
  (let [database (:database schema)
        table (:table schema)]
    (create-metadata client schema)
    (if (database-exists? client database)
      (log/warn database "already exists")
      (create-database client database))
    (if (container-exists? client database table)
      (log/warn (get-fullname database table) "already exists")
      (do
        (create-container client database table
                          (if (:ru schema) (:ru schema) ru))
        (register-stored-procedure client database table)))))

(defn- add-transaction-columns
  [schema]
  (let [s (merge (:columns schema) TRANSACTION_METADATA_COLUMNS)]
    (->> (reduce (fn [m [name type]]
                   (if (or (contains? (:partition-key schema) name)
                           (contains? (:clustering-key schema) name))
                     m
                     (assoc m (str "before_" name) type)))
                 {} s)
         (merge s))))

(defn create-transaction-table
  [client schema opts]
  (create-table client
                (merge schema {:columns (add-transaction-columns schema)})
                opts)
  (create-table client COORDINATOR_SCHEMA opts))

(defn- format-schema
  [schema]
  (map (fn [[k v]]
         (let [[db tbl] (clojure.string/split (name k) #"\.")
               v' (merge v {:columns (reduce-kv (fn [m c t]
                                                  (assoc m
                                                         (name c)
                                                         (.toLowerCase t)))
                                                {} (:columns v))})]
           (assoc v' :database db :table tbl)))
       schema))

(defn create-tables
  [client schema-file opts]
  (->> (cheshire/parse-stream (clojure.java.io/reader schema-file) true
                              #(when (or (= % "partition-key")
                                         (= % "clustering-key")) #{}))
       format-schema
       (map #(if (:transaction %)
               (create-transaction-table client % opts)
               (create-table client % opts)))
       doall))

(defn delete-all
  [client]
  (log/warn "Deleting all databases and tables")
  (mapv #(->> (.getId %) (.getDatabase client) .delete)
        (.readAllDatabases client)))
