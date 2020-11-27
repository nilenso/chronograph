(ns chronograph-web.pages.admin.db
  (:require [chronograph-web.db :as db]
            [chronograph-web.db.organization-context :as org-ctx-db]))

(defn user-is-admin?
  [db]
  (= "admin"
     (:role (org-ctx-db/current-organization db))))

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
  (:id (org-ctx-db/current-organization db)))

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
  (get-in db [:invited-members (current-org-id db)]))

(defn get-joined-members
  [db]
  (get-in db [:joined-members (current-org-id db)]))

(defn set-show-update-task-form
  [db task-id show?]
  (db/set-in-page-state db [:show-update-task-form? task-id] show?))

(defn show-update-task-form?
  [db task-id]
  (db/get-in-page-state db [:show-update-task-form? task-id]))
