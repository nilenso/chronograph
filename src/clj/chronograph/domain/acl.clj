(ns chronograph.domain.acl
  (:require [chronograph.db.acl :as db-acl]))

(def admin "admin")
(def member "member")

(defn create! [tx {:acls/keys [role] :as attributes}]
  (db-acl/create! tx
                  (select-keys attributes
                               [:acls/user-id
                                :acls/organization-id
                                :acls/role])))

(defn role [tx user-id organization-id]
  (->> {:acls/user-id user-id
        :acls/organization-id organization-id}
       (db-acl/find-by tx)
       :acls/role))

(defn admin? [tx user-id organization-id]
  (= admin (role tx user-id organization-id)))

(defn belongs-to-org? [tx user-id organization-id]
  (some? (role tx user-id organization-id)))
