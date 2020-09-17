(ns chronograph.db.user
  (:require [next.jdbc.sql :as sql]
            [chronograph.db.core :as db]
            [chronograph.utils.time :as time]))

(defn create!
  ([name email photo-url google-profiles-id]
   (create! db/datasource name email photo-url google-profiles-id))
  ([tx name email photo-url google-profiles-id]
   (let [now (time/now)]
     (sql/insert! tx
                  :users
                  {:name               name
                   :email              email
                   :photo-url          photo-url
                   :google-profiles-id google-profiles-id
                   :created-at         now
                   :updated-at         now}
                  db/sql-opts))))

(defn find-by-google-id
  ([google-id]
   (find-by-google-id db/datasource google-id))
  ([tx google-id]
   (first (sql/query tx
                     ["SELECT u.* FROM users u
                       INNER JOIN google_profiles gp ON u.google_profiles_id=gp.id
                       AND gp.google_id=?" google-id]
                     db/sql-opts))))

(defn find-by-id
  ([user-id]
   (find-by-id db/datasource user-id))
  ([tx user-id]
   (first (sql/query tx
                     ["SELECT * FROM users WHERE id=?" user-id]
                     db/sql-opts))))
