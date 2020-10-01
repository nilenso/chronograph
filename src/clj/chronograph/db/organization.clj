(ns chronograph.db.organization
  (:require [chronograph.db.core :as db]
            [next.jdbc.sql :as sql]
            [chronograph.utils.time :as time])
  (:refer-clojure :exclude [find]))

(defn create!
  ([organization]
   (create! db/datasource organization))
  ([tx {:organizations/keys [name slug]}]
   (let [now (time/now)]
     (sql/insert! tx
                  :organizations
                  {:name name
                   :slug slug
                   :created-at now
                   :updated-at now}
                  db/sql-opts))))

(defn where [tx attributes]
  (sql/find-by-keys tx :organizations attributes db/sql-opts))

(defn find [tx attributes]
  (first (where tx attributes)))

(defn find-by-slug [tx slug]
  (find tx {:slug slug}))

(defn by-ids [tx ids]
  (sql/query tx
             ["SELECT * FROM organizations where id = ANY(?)" (int-array ids)]
             db/sql-opts))
