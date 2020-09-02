(ns chronograph.db.google-profile
  (:require [next.jdbc.sql :as sql]
            [chronograph.db.core :as db]
            [chronograph.utils.time :as time]))

(defn create!
  ([google-id]
   (create! db/datasource google-id))
  ([tx google-id]
   (let [now (time/now)]
     (sql/insert! tx
                  :google-profiles
                  {:google-id  google-id
                   :created-at now
                   :updated-at now}
                  db/sql-opts))))
