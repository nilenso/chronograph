(ns chronograph.db.user
  (:require [next.jdbc.sql :as sql]
            [chronograph.db.core :as db]
            [chronograph.utils.time :as time]))

(defn create! [tx name email photo-url google-profiles-id]
  (let [now (time/now)]
    (sql/insert! tx
                 :users
                 {:name               name
                  :email              email
                  :photo-url          photo-url
                  :google-profiles-id google-profiles-id
                  :created-at         now
                  :updated-at         now}
                 db/sql-opts)))

(defn find-by-google-id [tx google-id]
  (first (sql/query tx
                    ["SELECT u.* FROM users u
                     INNER JOIN google_profiles gp ON u.google_profiles_id=gp.id
                     AND gp.google_id=?" google-id]
                    db/sql-opts)))

(defn find-by-id [tx user-id]
  (first (sql/query tx
                    ["SELECT * FROM users WHERE id=?" user-id]
                    db/sql-opts)))

(defn find-by-org-id
  [tx organization-id]
  (sql/query tx
             ["SELECT users.* FROM users, acls
                WHERE users.id = acls.user_id
                AND acls.organization_id=?" organization-id]
             db/sql-opts))
