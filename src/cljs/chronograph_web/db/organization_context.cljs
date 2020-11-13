(ns chronograph-web.db.organization-context
  (:require [chronograph-web.db.organization :as org-db]))

(defn current-organization-slug
  [db]
  ;; We're depending on route-params to fetch the organization slug. This
  ;; is iffy because of the implicit dependency on how the route is defined.
  (get-in db [:page :route-params :slug]))

(defn current-organization
  [db]
  (org-db/org-by-slug db (current-organization-slug db)))
