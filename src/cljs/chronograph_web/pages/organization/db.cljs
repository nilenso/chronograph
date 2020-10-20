(ns chronograph-web.pages.organization.db
  (:require [chronograph-web.db :as db]
            [chronograph-web.db.organization :as org-db]))

(defn slug
  [db]
  (get-in db [:page :route-params :slug]))

(defn current-org
  [db]
  (let [org-slug (slug db)]
    (org-db/org-by-slug db org-slug)))

(defn add-invited-member
  [db {:keys [organization-id] :as member}]
  (db/add-to-set-in db
                    [:invited-members organization-id]
                    member))

(defn add-invited-members
  [db members]
  (reduce add-invited-member
          db
          members))

(defn current-org-id
  [db]
  (:id (current-org db)))

(defn add-joined-member
  [db member]
  (db/add-to-set-in db
                    [:joined-members (current-org-id db)]
                    member))

(defn add-joined-members
  [db members]
  (reduce add-joined-member
          db
          members))

(defn get-invited-members
  [db]
  (get-in db [:invited-members (:id (current-org db))]))

(defn get-joined-members
  [db]
  (get-in db [:joined-members (current-org-id db)]))
