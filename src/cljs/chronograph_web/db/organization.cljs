(ns chronograph-web.db.organization
  (:require [chronograph-web.db :as db]))

(defn org-by-slug
  [db slug]
  (get-in db [:organizations slug]))

(defn org-id
  [db slug]
  (:id (org-by-slug db slug)))

(defn add-org
  [db {:keys [slug] :as organization}]
  (assoc-in db [:organizations slug] organization))

(defn add-organizations
  [db orgs]
  (update db :organizations merge (db/normalize-by :slug orgs)))

(defn organizations
  [db]
  (-> db
      :organizations
      vals))
