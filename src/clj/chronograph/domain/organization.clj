(ns chronograph.domain.organization
  (:require [chronograph.db.core :as db]
            [chronograph.db.organization :as db-organization]
            [chronograph.domain.acl :as acl]
            [next.jdbc :as jdbc]
            [clojure.spec.alpha :as s]))

(s/def :organizations/name string?)
(s/def :organizations/slug string?)
(s/def :organizations/id int?)

(s/def :organizations/create-params (s/keys :req [:organizations/name :organizations/slug]))
(s/def :organizations/organization (s/keys :req [:organizations/id
                                                 :organizations/name
                                                 :organizations/slug]))

(defn create! [organization owner-id]
  (jdbc/with-transaction [tx db/datasource]
    (let [{:keys [organizations/id] :as organization} (db-organization/create! tx organization)]
      (acl/create! tx {:user-id owner-id
                       :organization-id id
                       :role acl/admin})
      organization)))
