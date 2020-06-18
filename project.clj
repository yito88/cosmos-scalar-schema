(defproject cosmos-scalar-schema "0.1.0-SNAPSHOT"
  :description "Schema tool for Scalar DB on Cosmos DB"
  :url "http://github.com/yito88/cosmos-scalar-schema"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [com.azure/azure-cosmos "4.0.1"]]
  :java-source-paths ["java/src"]
  :repl-options {:init-ns cosmos-scalar-schema.core})
