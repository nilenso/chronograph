(ns chronograph.db.task
  (:require [next.jdbc.sql :as sql]
            [chronograph.db.core :as db]
            [chronograph.utils.time :as time]))

(defn create! [tx {:keys [name description organization-id]}]
  (let [now (time/now)]
    (sql/insert! tx
                 :tasks
                 {:name name
                  :description description
                  :organization-id organization-id
                  :created-at now
                  :updated-at now}
                 db/sql-opts)))

(defn where [tx attributes]
  (sql/find-by-keys tx :tasks attributes db/sql-opts))

(defn find-by-id [tx id]
  (first (where tx {:id id})))

(defn update! [tx id updates]
  (sql/update! tx :tasks
               (merge {:updated-at (time/now)} updates)
               {:id id}
               db/sql-opts))
