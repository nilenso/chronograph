(ns chronograph-web.pages.admin.subscriptions
  (:require [re-frame.core :as rf]
            [chronograph-web.pages.admin.db :as page-db]
            [chronograph-web.db.organization-context :as org-ctx-db]
            [chronograph-web.db.tasks :as tasks-db]))

(rf/reg-sub
  ::invited-members
  (fn [db _]
    (page-db/get-invited-members db)))

(rf/reg-sub
  ::joined-members
  (fn [db _]
    (page-db/get-joined-members db)))

(rf/reg-sub
  ::tasks
  (fn [db [_ _]]
    (tasks-db/current-organization-tasks db)))

(rf/reg-sub
  ::org-slug
  (fn [db _]
    (org-ctx-db/current-organization-slug db)))

(rf/reg-sub
  ::show-update-task-form?
  (fn [db [_ id]]
    (page-db/show-update-task-form? db id)))

(rf/reg-sub
  ::user-is-admin?
  (fn [db _]
    (page-db/user-is-admin? db)))
