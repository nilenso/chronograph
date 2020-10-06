(ns chronograph.domain.organization
  (:require [chronograph.domain.task :as task]
            [chronograph.db.organization :as db-organization]
            [chronograph.db.user :as db-user]
            [chronograph.domain.acl :as acl]))

(defn create! [tx organization owner-id]
  (let [{:keys [organizations/id] :as organization} (db-organization/create! tx organization)]
    (acl/create! tx {:acls/user-id owner-id
                     :acls/organization-id id
                     :acls/role acl/admin})
    organization))

(defn find-if-authorized
  [tx slug user-id]
  (when-let [{:organizations/keys [id]
              :as organization} (db-organization/find-by-slug tx slug)]
    (when (acl/belongs-to-org? tx
                               user-id
                               id)
      organization)))

(defn tasks [tx {:organizations/keys [id]}]
  (task/list tx {:organization-id id}))

(def find-by db-organization/find-by)

(defn for-user [tx {:users/keys [id] :as _user}]
  (db-organization/user-organizations tx id))

(def find-by-slug db-organization/find-by-slug)

(def members db-user/find-by-org-id)
