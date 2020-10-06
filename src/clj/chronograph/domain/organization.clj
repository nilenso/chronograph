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
    organization))

(defn find-if-authorized
  [tx slug user-id]
  (when-let [{:organizations/keys [id]
              :as organization} (db-organization/find-by
                                 tx
                                 {:organizations/slug slug})]
    (when (acl/belongs-to-org? tx user-id id)
      organization)))

(def find-by db-organization/find-by)

(defn find-by-slug [tx slug]
  (db-organization/find-by tx {:organizations/slug slug}))

(def members db-user/find-by-org-id)

(defn for-user [tx user]
  (db-organization/user-organizations tx (:users/id user)))
