(ns sortable-clj.core
  (:require [clojure.string       :as str]
            [clojure.java.io      :as io]
            [clojure.spec.alpha   :as s]
            [compojure.core       :refer [defroutes GET PUT]]
            [compojure.route      :as route]
            [next.jdbc.result-set :as rs]
            [next.jdbc            :as jdbc]
            [ring.adapter.jetty   :as jetty]
            [ring.util.response   :refer [response]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]])
  (:gen-class))

;; Data specifications for documentation
;; purpose.
(s/def ::id (s/nilable int?))
(s/def ::name string?)
(s/def ::position int?)
(s/def ::date inst?)

(s/def ::item {:req-un [::id
                        ::position]
               :opt-un [::name
                        ::date]})
(s/def ::items (s/coll-of ::item))
(s/def ::deleted (s/coll-of ::id))

(s/def ::put-items-request (s/keys :req-un [::deleted
                                            ::items]))

(def db-spec
  "Database credentials."
  {:dbtype "postgresql"
   :dbname "postgres"
   :user "postgres"
   :password "pass"})

(defn slurp-resource
  "Read a file from the classpath."
  [file]
  (slurp (io/resource file)))

(defn execute-query
  "Execute a SQL query on the database."
  [query]
  (jdbc/execute! db-spec [query] {:builder-fn rs/as-unqualified-kebab-maps}))

(def select-all-query "SELECT * FROM ITEMS ORDER BY POSITION")
(def insert-query "INSERT INTO ITEMS (POSITION, NAME, UPDATED) VALUES (?,?,NOW())")
(def update-query "UPDATE ITEMS SET POSITION=?,UPDATED=NOW() WHERE ID=?")
(def delete-query "DELETE FROM ITEMS WHERE ID=?")
(def reorder-items-query "
   UPDATE ITEMS AS T
   SET position=S.position
   FROM (SELECT id,(ROW_NUMBER() OVER (ORDER BY position,updated)) AS position  FROM ITEMS ORDER BY position,updated) AS S
   WHERE T.ID=S.ID AND T.POSITION<>S.POSITION")

(defn load-db
  "Execute script `init.sql` on the database (create the table)."
  []
  (run! execute-query (str/split (slurp-resource "init.sql") #";")))

(defn all-items
  "Get all the items from the database ordered by position."
  []
  (execute-query select-all-query))

(defn reorder-items
  "Reorder all the items by position on the database."
  [transaction]
  (-> (jdbc/execute! transaction [reorder-items-query])
      (first)
      (:next.jdbc/update-count)))

(defn generate-name
  "Generate a random name from the 8 first characters of a random
  `java.util.UUID`."
  []
  (format "Item %s" (str/join (take 8 (str (random-uuid))))))

(defn reset-items
  "Remove all the data from the database and generate `nb` new items."
  [nb]
  (load-db)
  (with-open [transaction (jdbc/get-connection db-spec)]
    (let [insert-params (map (fn [position] [(inc position) (generate-name)]) (range (parse-long nb)))]
      (jdbc/execute-batch! transaction insert-query insert-params {})
      nil)))

(defn put-items
  "Remove all the `deleted` items and then save the `items`
  on the database.
  At the end, ensure all the items are in correct order.
  Everything happens on a single transaction to ensure consistancy."
  [{:keys [items deleted]}]
  (with-open [transaction (jdbc/get-connection db-spec)]
    (doseq [id deleted]
      (jdbc/execute! transaction [delete-query id]))
    (let [[to-update to-insert] ((juxt filter remove) :id items)]
      (doseq [item to-update]
        (jdbc/execute! transaction [update-query
                                    (:position item)
                                    (:id item)]))
      (doseq [item to-insert]
        (jdbc/execute! transaction [insert-query
                                    (:position item)
                                    (or (:name item) (generate-name))])))
    (reorder-items transaction)))

(defroutes app
  (PUT "/items/reset/:nb" [nb] (response (str (reset-items nb))))
  (PUT "/items" req (response (str (put-items (:body req)))))
  (GET "/items" [] (response (all-items)))
  (route/not-found "Not found"))

(defn start-server []
  (jetty/run-jetty (wrap-json-response (wrap-json-body #'app {:keywords? true}))
                   {:join? false
                    :port 8080}))

(defn -main [& _]
  ;; Load the database
  (load-db)
  ;; Start the server
  (start-server))
