(ns chronograph.domain.organization
  (:require [chronograph.db.core :as db]
            [chronograph.db.organization :as db-organization]
            [chronograph.domain.acl :as acl]
            [next.jdbc :as jdbc]
            [clojure.spec.alpha :as s]))

(defn create! [organization owner-id]
  (jdbc/with-transaction [tx db/datasource]
    (let [{:keys [organizations/id] :as organization} (db-organization/create! tx organization)]
      (acl/create! tx {:user-id owner-id
                       :organization-id id
                       :role acl/admin})
      organization)))

(def find-one db-organization/find-one)
