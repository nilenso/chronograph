(ns chronograph-web.pages.create-organization.db
  (:require [chronograph-web.db.organization :as org-db]))

(defn add-to-organizations
  [db organization]
  (org-db/add-org db organization))
