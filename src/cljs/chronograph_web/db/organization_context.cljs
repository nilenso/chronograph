(ns chronograph-web.db.organization-context
  (:require [chronograph-web.db.organization :as org-db]))

(defn current-organization-slug
  [db]
  (get-in db [:page :route-params :slug]))

(defn current-organization
  [db]
  (org-db/org-by-slug db (current-organization-slug db)))
