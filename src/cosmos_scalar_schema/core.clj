(ns cosmos-scalar-schema.core
  (:require [clojure.tools.cli :refer [parse-opts]]
            [cosmos-scalar-schema.schema :as schema])
  (:gen-class))

(def cli-options
  [["-u" "--uri ACCOUNT_URI" "URI address of your Cosmos DB account"]
   ["-p" "--password ACCOUNT_PASSWORD" "Password of your Cosmos DB account"]
   ["-f" "--schema-file SCHEMA_JSON" "Schema file"]
   ["-r" "--ru RESOURCE_UNIT" "Base RU for each table. The RU of the coordinator for Scalar DB transaction is specified by this option."
    :default 400 :parse-fn #(Integer/parseInt %)]
   ["-D" "--delete-all" "All database will be deleted. If this is enabled, -s option will be ignored."]
   ["-h" "--help"]])

(defn -main [& args]
  (let [{:keys [options summary errors]
         {:keys [schema-file uri password help]} :options}
        (parse-opts args cli-options)]
    (if (or help errors)
      (do (when (not help)
            (println (str "ERROR: " errors)))
          (println summary))
      (with-open [client (schema/get-client uri password)]
        (if (:delete-all options)
          (schema/delete-all client)
          (schema/create-tables client schema-file options))))))
