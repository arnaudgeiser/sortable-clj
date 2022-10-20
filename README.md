# Sortable

A small experiment to prove sortable problem can be solve
easily with just a little bit of SQL.

## Prerequisite

- [Docker](https://docs.docker.com/get-docker/)
- [Leiningen](https://leiningen.org/)

## Run

``` sh
# Start PostgreSQL
docker run --rm --name postgres -p 5432:5432 -e POSTGRES_PASSWORD=pass -d postgres

# Run the HTTP server
lein run
``` 

## Experiment

__Generate 20 ordered items__
```shell
curl -X PUT localhost:8080/items/reset/20
```

__Get all the items__
```shell
curl localhost:8080/items
```

__Remove an item in the middle__
```shell
curl -X PUT localhost:8080/items -H "Content-Type: application/json" -d '{"deleted": [10]}"'
10 # => 10 rows affected
```

__Insert a new item at the end__
```shell
curl -X PUT localhost:8080/items -H "Content-Type: application/json" -d '{"items" : [{"name": "New Item", "position": 20}]}"'
```

__Move this item to second to last__
```shell
curl -X PUT localhost:8080/items -H "Content-Type: application/json" -d '{"items" : [{"id": 21, "position": 19}]}"'
1 # => 1 row affected
```
