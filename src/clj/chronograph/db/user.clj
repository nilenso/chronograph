(ns chronograph.db.user
  (:require [chronograph.db.core :as db]))

(defn create! [tx {:users/keys [name
                                email
                                photo-url
                                google-profiles-id]}]
  (db/create! :users
              tx
              {:users/name name
               :users/email email
               :users/photo-url photo-url
               :users/google-profiles-id google-profiles-id}))

(defn find-by [tx attributes]
  (db/find-by :users
              tx
              attributes))

(defn find-by-google-id [tx google-id]
  (first (db/query tx
                   ["SELECT u.* FROM users u
                     INNER JOIN google_profiles gp ON u.google_profiles_id=gp.id
                     AND gp.google_id=?" google-id])))

(defn find-by-org-id
  [tx organization-id]
  (db/query tx
            ["SELECT users.* FROM users, acls
                WHERE users.id = acls.user_id
                AND acls.organization_id=?" organization-id]))
