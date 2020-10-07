(ns chronograph.domain.user
  (:require [chronograph.db.google-profile :as google-profile-db]
            [chronograph.db.user :as user-db]))

(def find-by-google-id user-db/find-by-google-id)

(defn find-by-id [tx user-id]
  (user-db/find-by tx {:users/id user-id}))

(defn find-or-create-google-user!
  [tx google-id name email photo-url]
  (if-let [google-user (find-by-google-id tx google-id)]
    google-user
    (let [{google-profiles-id :google-profiles/id}
          (google-profile-db/create! tx {:google-profiles/google-id google-id})]
      (user-db/create! tx
                       {:users/name name
                        :users/email  email
                        :users/photo-url photo-url
                        :users/google-profiles-id google-profiles-id}))))
