(ns chronograph-web.db.organization
  (:require [chronograph-web.db :as db]))

(defn org-by-slug
  [db slug]
  (get-in db [:organizations slug]))

(defn by-id
  [db id]
  (->> db
       :organizations
       vals
       (filter #(= id (:id %)))
       first))

(defn org-id
  [db slug]
  (:id (org-by-slug db slug)))

(defn add-org
  [db {:keys [slug] :as organization}]
  (assoc-in db [:organizations slug] organization))

(defn set-organizations
  [db orgs]
  (assoc db :organizations (db/normalize-by :slug orgs)))

(defn organizations
  [db]
  (->> db
       :organizations
       vals
       (sort-by :slug)))
