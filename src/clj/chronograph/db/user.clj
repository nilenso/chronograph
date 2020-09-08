(ns chronograph.db.user
  (:require [next.jdbc.sql :as sql]
            [chronograph.db.core :as db]
            [chronograph.utils.time :as time]))

(defn create!
  ([name email photo-url]
   (create! db/datasource name email photo-url))
  ([tx name email photo-url]
   (let [now (time/now)]
     (sql/insert! tx
                  :users
                  {:name       name
                   :email      email
                   :photo-url  photo-url
                   :created-at now
                   :updated-at now}
                  db/sql-opts))))

(defn find-by-google-id
  ([google-id]
   (find-by-google-id db/datasource google-id))
  ([tx google-id]
   (first (sql/query tx
                     ["SELECT u.id, u.name, u.email, u.photo_url, u.created_at, u.updated_at FROM users u
                       INNER JOIN linked_profiles lp ON u.id=lp.user_id
                       INNER JOIN google_profiles gp ON lp.profile_id=gp.id
                       WHERE lp.profile_type='google'
                       AND   gp.google_id=?" google-id]
                     db/sql-opts))))

(defn find-by-id
  ([user-id]
   (find-by-id db/datasource user-id))
  ([tx user-id]
   (first (sql/query tx
                     ["SELECT * FROM users WHERE id=?" user-id]
                     db/sql-opts))))
