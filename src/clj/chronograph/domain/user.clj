(ns chronograph.domain.user
  (:require [chronograph.db.core :as db]
            [next.jdbc :as jdbc]
            [chronograph.db.google-profile :as google-profile-db]
            [chronograph.db.user :as user-db]))

(def find-by-google-id user-db/find-by-google-id)

(def find-by-id user-db/find-by-id)

(defn find-or-create-google-user!
  [google-id name email photo-url]
  (jdbc/with-transaction [tx db/datasource]
    (if-let [google-user (find-by-google-id tx google-id)]
      google-user
      (let [{google-profiles-id :google-profiles/id} (google-profile-db/create! tx google-id)
            user (user-db/create! tx name email photo-url google-profiles-id)]
        user))))
