# Schema Tool for Scalar DB on Cosmos DB
This tool makes schemas on Cosmos DB for Scalar DB
  - This creates databases(collections) and tables(containers), also inserts metadata which is required by Scalar DB

# Usage

## Build & Run
### Build a standalone jar
```console
$ lein uberjar
```

### Create tables
```console
$ java -jar target/cosmos-scalar-schema.jar -u <YOUR_ACCOUNT_URI> -p <YOUR_ACCOUNT_PASSWORD> -s schema.json [-r BASE_RESOURCE_UNIT]
```
  - `-r BASE_RESOURCE_UNIT` is an option. You can specify the RU of each table. When you use transaction function, the RU of the coordinator table of Scalar DB is specified by this. By default it's 400.

### Delete all tables
```console
$ java -jar target/cosmos-scalar-schema.jar -D
```

### Sample schema file
```json
{
  "sample-db.sample-table1": {
    "transaction": true,
    "partition-key": [
      "c1"
    ],
    "clustering-key": [
      "c4"
    ],
    "columns": {
      "c1": "INT",
      "c2": "TEXT",
      "c3": "INT",
      "c4": "INT",
      "c5": "BOOLEAN"
    },
    "ru": 400
  },

  "sample-db.sample-table2": {
    "transaction": false,
    "partition-key": [
      "c1"
    ],
    "clustering-key": [
      "c3",
      "c4"
    ],
    "columns": {
      "c1": "INT",
      "c2": "TEXT",
      "c3": "INT",
      "c4": "INT",
      "c5": "BOOLEAN"
    },
    "ru": 800
  }
}
```

## REPL
1. Run REPL
    ```console
    $ lein repl
    ```

2. Make a table for Scalar DB
    ```clojure
    (def client (get-client your-account your-password))

    (create-table client
                  {:database "sample-db"
                   :table "sample-table"
                   :partition-key #{"c1"}
                   :clustering-key #{"c4"}
                   :columns {"c1" "int"
                             "c2" "text"
                             "c3" "int"
                             "c4" "int"
                             "c5" "boolean"}
                   :ru 400}
                   {})

    (create-transaction-table client
                              {:database "sample-db"
                               :table "sample-table"
                               :partition-key #{"c1"}
                               :clustering-key #{"c4"}
                               :columns {"c1" "int"
                                         "c2" "text"
                                         "c3" "int"
                                         "c4" "int"
                                         "c5" "boolean"}
                               :ru 1000}
                               {:ru 800})
    ```
    - RU can be set for each table to set `:ru` in a table configuration. By default, it is 400.
    - The RU of the coordinator table can be specified the `:ru` in the second argument.
