(ns chronograph.db.task
  (:require [next.jdbc.sql :as sql]
            [chronograph.db.core :as db]
            [chronograph.utils.time :as time]))

(defn create!
  ([task]
   (create! db/datasource task))
  ([tx {:keys [name description organization-id]}]
   (let [now (time/now)]
     (sql/insert! tx
                  :tasks
                  {:name name
                   :description description
                   :organization-id organization-id
                   :created-at now
                   :updated-at now}
                  db/sql-opts))))

(defn where
  ([options]
   (where db/datasource options))
  ([tx options]
   (sql/find-by-keys tx :tasks options db/sql-opts)))

(defn find-by-id
  ([id]
   (find-by-id db/datasource id))
  ([tx id]
   (first (where tx {:id id}))))

(defn update! [tx id updates]
  (sql/update! tx :tasks
               (merge {:updated-at (time/now)} updates )
               {:id id}
               db/sql-opts))
