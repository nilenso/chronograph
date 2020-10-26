(ns chronograph.domain.organization
  (:require [chronograph.db.organization :as db-organization]
            [chronograph.db.user :as db-user]
            [chronograph.domain.acl :as acl]))

(defn create! [tx organization owner-id]
  (let [{:keys [organizations/id] :as organization}
        (db-organization/create! tx (select-keys organization
                                                 [:organizations/name
                                                  :organizations/slug]))]
    (acl/create! tx {:acls/user-id owner-id
                     :acls/organization-id id
                     :acls/role acl/admin})
    (assoc organization
           :acls/role acl/admin)))

(defn find-if-authorized
  [tx slug user-id]
  (when-let [{:organizations/keys [id]
              :as organization} (db-organization/find-by
                                 tx
                                 {:organizations/slug slug})]
    (when-let [role (acl/role tx user-id id)]
      (assoc organization :acls/role role))))

(def find-by db-organization/find-by)

(def members db-user/find-by-org-id)

(defn find-invited
  [tx email]
  (db-organization/invited-organizations tx email))

(defn for-user [tx user]
  (db-organization/user-organizations-with-role tx (:users/id user)))
