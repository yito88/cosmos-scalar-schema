# Schema Tool for Scalar DB on Cosmos DB
- Scalar DB on Cosmos DB is experimental for now

# Usage
1. Set environment variables `COSMOS_URI` and `COSMOS_PASSWORD` for your Cosmos DB account
```
$ export COSMOS_URI=https://<your_account>.documents.azure.com:443/
% export COSMOS_PASSWORD=<your_account_password>
```

2. Run REPL
```
$ lein repl
```

3. Make a table for Scalar DB
```clojure
(create-table {:database "sample-db"
               :table "sample-table"
               :partition-key #{"c1"}
               :clustering-key #{"c4"}
               :columns {"c1" "int"
                         "c2" "text"
                         "c3" "int"
                         "c4" "int"
                         "c5" "boolean"}
               :ru 400})
```
