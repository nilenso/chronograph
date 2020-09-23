(ns chronograph.domain.organization
  (:require [chronograph.db.core :as db]
            [chronograph.db.organization :as db-organization]
            [chronograph.db.user :as db-user]
            [chronograph.domain.acl :as acl]
            [next.jdbc :as jdbc]))

(defn create! [organization owner-id]
  (jdbc/with-transaction [tx db/datasource]
    (let [{:keys [organizations/id] :as organization} (db-organization/create! tx organization)]
      (acl/create! tx {:user-id owner-id
                       :organization-id id
                       :role acl/admin})
      organization)))

(defn find-if-authorized
  [slug user-id]
  (jdbc/with-transaction [tx db/datasource]
    (when-let [{:organizations/keys [id]
                :as organization} (db-organization/find-by-slug tx slug)]
      (when (acl/belongs-to-org? tx
                                 user-id
                                 id)
        organization))))

(def find-by-slug db-organization/find-by-slug)

(def members db-user/find-by-org-id)
