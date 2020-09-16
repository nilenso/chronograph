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

(defn find-by-id
  ([id]
   (find-by-id db/datasource id))
  ([tx id]
   (first (sql/query tx
                     ["SELECT * FROM tasks WHERE id=?" id]
                     db/sql-opts))))
