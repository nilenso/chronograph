(ns chronograph.db.linked-profile
  (:require [next.jdbc.sql :as sql]
            [chronograph.db.core :as db]
            [chronograph.utils.time :as time]))

(defn create!
  ([user-id profile-id profile-type]
   (create! db/datasource user-id profile-id profile-type))
  ([tx user-id profile-id profile-type]
   (let [now (time/now)]
     (sql/insert! tx
                  :linked-profiles
                  {:user-id      user-id
                   :profile-type profile-type
                   :profile-id   profile-id
                   :created-at   now
                   :updated-at   now}
                  db/sql-opts))))
