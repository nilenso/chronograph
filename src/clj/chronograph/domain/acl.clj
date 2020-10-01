(ns chronograph.domain.acl
  (:require [chronograph.db.acl :as db-acl]))

(def admin "admin")
(def member "member")

(def create! db-acl/create!)

(defn role [tx user organization]
  (->> {:user-id (:users/id user)
        :organization-id (:organizations/id organization)}
       (db-acl/find-by tx)
       :acls/role))

(defn admin? [tx user-id organization-id]
  (= admin (role tx
                 {:users/id user-id}
                 {:organizations/id organization-id})))

(defn belongs-to-org? [tx user-id organization-id]
  (some?
   (role tx
         {:users/id user-id}
         {:organizations/id organization-id})))
