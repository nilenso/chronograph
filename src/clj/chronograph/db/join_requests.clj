(ns chronograph.db.join-requests
  (:require [chronograph.db.core :as db]
            [next.jdbc.sql :as sql]
            [chronograph.utils.time :as time]))

(defn create!
  ([join-request]
   (create! db/datasource join-request))
  ([tx join-request]
   (let [now (time/now)]
     (sql/insert! tx
                  :join-requests
                  (merge join-request
                         {:created-at now
                          :updated-at now})
                  db/sql-opts))))

(defn accept! [tx organization-id user-id]
  (let [now (time/now)]
    (sql/update! tx
                 :join-requests
                 {:organization-id organization-id
                  :user-id user-id}
                 {:accepted-at now}
                 db/sql-opts)))
