(ns chronograph-web.pages.organization.db
  (:require [chronograph-web.db :as db]))

(defn get-from-add-member-form
  [db form-key]
  (db/get-in-page-state db [:add-member-form form-key]))

(defn set-in-add-member-form
  [db form-key value]
  (db/set-in-page-state db [:add-member-form form-key] value))

(defn slug
  [db]
  (get-in db [:page :route-params :slug]))

(defn org-by-slug
  [db slug]
  (get-in db [:organizations slug]))

(defn current-org
  [db]
  (let [org-slug (slug db)]
    (org-by-slug db org-slug)))

(defn add-invited-member
  [db org-id email]
  (db/add-to-set-in db
                    [:invited-members org-id]
                    {:organization-id org-id
                     :email           email}))

(defn add-invited-members
  [db members]
  (reduce (fn [db {:keys [organization-id email]}]
            (add-invited-member db organization-id email))
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
