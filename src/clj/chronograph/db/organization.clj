(ns chronograph.db.organization
  (:require [chronograph.db.core :as db]
            [next.jdbc.sql :as sql]
            [chronograph.utils.time :as time]))

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

(defn where
  ([options]
   (where db/datasource options))
  ([tx options]
   (sql/find-by-keys tx :organizations options db/sql-opts)))

(defn find [tx options]
  (first (where tx options)))

(defn find-by-slug
  ([slug]
   (find-by-slug db/datasource slug))
  ([tx slug]
   (find tx {:slug slug})))

(defn by-ids [tx ids]
  (sql/query tx
             ["SELECT * FROM organizations where id = ANY(?)" (int-array ids)]
             db/sql-opts))
