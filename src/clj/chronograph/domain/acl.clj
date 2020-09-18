(ns chronograph.domain.acl
  (:require [chronograph.db.acl :as db-acl]
            [chronograph.db.core :as db]))

(def admin "admin")
(def member "member")

(def create! db-acl/create!)

(defn admin? [user-id organization-id]
  (= admin
     (get (db-acl/find-acl user-id organization-id) :acls/role)))

(defn belongs-to-org?
  ([user-id organization-id]
   (belongs-to-org? db/datasource user-id organization-id))
  ([tx user-id organization-id]
   (some? (db-acl/find-acl tx user-id organization-id))))
