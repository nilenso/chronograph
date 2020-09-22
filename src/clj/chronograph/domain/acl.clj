(ns chronograph.domain.acl
  (:require [chronograph.db.acl :as db-acl]
            [chronograph.db.core :as db]))

(def admin "admin")
(def member "member")

(def create! db-acl/create!)

(defn admin? [tx user-id organization-id]
  (if-let [acl (db-acl/find-one tx {:user-id user-id
                                    :organization-id organization-id})]
    (= admin (get acl :acls/role))
    false))

(defn belongs-to-org? [tx user-id organization-id]
  (some? (db-acl/find-one tx {:user-id user-id
                              :organization-id organization-id})))
