(defproject cosmos-scalar-schema "0.1.0-SNAPSHOT"
  :description "Schema tool for Scalar DB on Cosmos DB"
  :url "http://github.com/yito88/cosmos-scalar-schema"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/tools.cli "1.0.194"]
                 [org.clojure/tools.logging "1.1.0"]
                 [org.apache.logging.log4j/log4j-core "2.13.3"]
                 [org.slf4j/slf4j-log4j12 "1.7.30"]
                 [cheshire "5.10.0"]
                 [com.azure/azure-cosmos "4.1.0"]
                 [com.google.guava/guava "24.1-jre"]]
  :resource-paths ["resources"
                   "stored_procedure"]
  :java-source-paths ["java/src"]
  :repl-options {:init-ns cosmos-scalar-schema.schema}
  :main cosmos-scalar-schema.core
  :profiles {:uberjar {:aot :all
                       :uberjar-name "cosmos-scalar-schema.jar"
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
