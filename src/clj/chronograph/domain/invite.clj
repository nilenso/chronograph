(ns chronograph.domain.invite
  (:require [chronograph.db.core :as db]
            [chronograph.db.invite :as db-invite]
            [chronograph.db.organization :as db-organization]
            [next.jdbc :as jdbc]))

(defn create!
  [slug email]
  (jdbc/with-transaction [tx db/datasource]
    (let [organization-id (:organizations/id (db-organization/find-by-slug tx slug))]
      (db-invite/create! tx organization-id email))))

(def find-by-org-id db-invite/find-by-org-id)
