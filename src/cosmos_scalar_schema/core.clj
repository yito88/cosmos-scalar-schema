(ns cosmos-scalar-schema.core
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

(def ^:const ^String STORED_PROCEDURE_DIR "stored-procedure/")
(def ^:const REGISTERED_STORED_PROCEDURES ["putIf.js"
                                           "putIfNotExists.js"
                                           "deleteIf.js"])

(def SAMPLE_SCHEMA {:database "sample-db"
                    :table "sample-table"
                    :partition-key #{"c1"}
                    :clustering-key #{"c4"}
                    :columns {"c1" "int"
                              "c2" "text"
                              "c3" "int"
                              "c4" "int"
                              "c5" "boolean"}
                    :ru 400})

(defn- get-fullname
  [database container]
  (str database "." container))

(defn get-client
  []
  (.buildClient (doto (CosmosClientBuilder.)
                  (.endpoint (System/getenv "COSMOS_URI"))
                  (.key (System/getenv "COSMOS_PASSWORD"))
                  (.consistencyLevel ConsistencyLevel/STRONG)
                  .directMode)))

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
  (create-database client METADATA_DATABASE)
  (let [metadata (doto (TableMetadata.)
                   (.setId (get-fullname (:database schema) (:table schema)))
                   (.setPartitionKeyNames (:partition-key schema))
                   (.setClusteringKeyNames (:clustering-key schema))
                   (.setColumns (:columns schema)))]
    (create-container client METADATA_DATABASE METADATA_CONTAINER 400)
    (-> (.getDatabase client METADATA_DATABASE)
        (.getContainer METADATA_CONTAINER)
        (.createItem metadata))))

(defn register-stored-procedures
  [client database container]
  (let [scripts (-> client (.getDatabase database) (.getContainer container)
                    .getScripts)]
    (map #(.createStoredProcedure scripts
                                  (CosmosStoredProcedureProperties.
                                   % (slurp (str STORED_PROCEDURE_DIR %)))
                                  (CosmosStoredProcedureRequestOptions.))
         REGISTERED_STORED_PROCEDURES)))

(defn create-table
  [schema]
  (with-open [client (get-client)]
    (create-metadata client schema)
    (create-database client (:database schema))
    (create-container client (:database schema) (:table schema)
                      (if (:ru schema) 400 (:ru schema)))
    (register-stored-procedures client (:database schema) (:table schema))))
