(ns chronograph.domain.user
  (:require [chronograph.db.core :as db]
            [next.jdbc :as jdbc]
            [chronograph.db.google-profile :as google-profile-db]
            [chronograph.db.linked-profile :as linked-profile-db]
            [chronograph.db.user :as user-db]))

(def find-by-google-id user-db/find-by-google-id)

(def find-by-id user-db/find-by-id)

(defn- link-google-profile!
  [tx user-id google-id]
  (let [{google-profiles-id :id} (google-profile-db/create! tx google-id)]
    (linked-profile-db/create! tx user-id google-profiles-id "google")))

(defn find-or-create-google-user!
  [google-id name email photo-url]
  (jdbc/with-transaction [tx db/datasource]
    (if-let [google-user (find-by-google-id tx google-id)]
      google-user
      (let [{user-id :id} (user-db/create! tx name email photo-url)]
        (link-google-profile! tx user-id google-id)
        {:id        user-id
         :name      name
         :email     email
         :photo-url photo-url}))))
