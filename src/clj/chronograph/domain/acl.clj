(ns chronograph.domain.acl
  (:require [chronograph.db.acl :as db-acl]))

(def admin "admin")
(def member "member")

(def create! db-acl/create!)

(defn admin? [tx user-id organization-id]
  (= admin (->> {:user-id user-id :organization-id organization-id}
                (db-acl/find tx)
                :acls/role)))

(defn belongs-to-org? [tx user-id organization-id]
  (some? (db-acl/find tx {:user-id user-id
                          :organization-id organization-id})))
