(ns chronograph.db.users
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [next.jdbc.result-set :as jdbc-result-set]
            [next.jdbc :as jdbc]
            [chronograph.db.core :as db]
            [camel-snake-kebab.core :as csk])
  (:import (java.time Instant)))

(def ^:private sql-opts {:builder-fn jdbc-result-set/as-unqualified-kebab-maps
                         :column-fn  csk/->snake_case_string
                         :table-fn   csk/->snake_case_string})

(defn- create-user!
  ([name email photo-url]
   (create-user! db/datasource name email photo-url))
  ([tx name email photo-url]
   (let [now (Instant/now)]
     (sql/insert! tx
                  :users
                  {:name       name
                   :email      email
                   :photo-url  photo-url
                   :created-at now
                   :updated-at now}
                  sql-opts))))

(defn- create-google-profile!
  ([google-id]
   (create-google-profile! db/datasource google-id))
  ([tx google-id]
   (let [now (Instant/now)]
     (sql/insert! tx
                  :google-profiles
                  {:google-id  google-id
                   :created-at now
                   :updated-at now}
                  sql-opts))))

(defn- create-linked-profile!
  ([user-id profile-id profile-type]
   (create-linked-profile! db/datasource user-id profile-id profile-type))
  ([tx user-id profile-id profile-type]
   (let [now (Instant/now)]
     (sql/insert! tx
                  :linked-profiles
                  {:user-id      user-id
                   :profile-type profile-type
                   :profile-id   profile-id
                   :created-at   now
                   :updated-at   now}
                  sql-opts))))

(defn link-google-profile!
  [tx user-id google-id]
  (let [{google-profiles-id :id} (create-google-profile! tx google-id)]
    (create-linked-profile! tx user-id google-profiles-id "google")))

(defn find-by-google-id
  ([google-id]
   (find-by-google-id db/datasource google-id))
  ([tx google-id]
   (first (sql/query tx
                     ["SELECT u.id, u.name, u.email, u.photo_url FROM users u
                       INNER JOIN linked_profiles lp ON u.id=lp.user_id
                       INNER JOIN google_profiles gp ON lp.profile_id=gp.id
                       WHERE lp.profile_type='google'
                       AND   gp.google_id=?" google-id]
                     sql-opts))))

(defn find-or-create-google-user!
  [google-id name email photo-url]
  (jdbc/with-transaction [tx db/datasource]
    (if-let [google-user (find-by-google-id tx google-id)]
      google-user
      (let [{user-id :id} (create-user! tx name email photo-url)]
        (link-google-profile! tx user-id google-id)
        {:id        user-id
         :name      name
         :email     email
         :photo-url photo-url}))))

(defn find-by-id
  ([user-id]
   (find-by-id db/datasource user-id))
  ([tx user-id]
   (first (sql/query tx
                     ["SELECT id, name, email, photo_url FROM users WHERE id=?" user-id]
                     sql-opts))))
