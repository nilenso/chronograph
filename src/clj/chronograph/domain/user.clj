(ns chronograph.domain.user
  (:require [chronograph.db.google-profile :as google-profile-db]
            [chronograph.db.acl :as db-acl]
            [chronograph.db.organization :as org-db]
            [chronograph.db.user :as user-db]))

(def find-by-google-id user-db/find-by-google-id)

(def find-by-id user-db/find-by-id)

(defn find-or-create-google-user!
  [tx google-id name email photo-url]
  (if-let [google-user (find-by-google-id tx google-id)]
    google-user
    (let [{google-profiles-id :google-profiles/id} (google-profile-db/create! tx google-id)
          user (user-db/create! tx name email photo-url google-profiles-id)]
      user)))

(defn organizations [tx {:users/keys [id] :as _user}]
  (let [acls (db-acl/where tx {:user-id id})]
    (org-db/by-ids tx (map :acls/organization-id acls))))
