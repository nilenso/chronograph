(ns chronograph-web.pages.landing.db
  (:require [chronograph-web.db.organization :as org-db]))

(defn add-to-organizations
  [db organization]
  (org-db/add-org db organization))
