(ns chronograph-web.pages.timers.subscriptions
  (:require [chronograph-web.db.organization-invites :as org-invites-db]
            [re-frame.core :as rf]
            [chronograph-web.db.timers :as timers-db]
            [chronograph-web.db.organization-context :as org-context-db]
            [chronograph-web.db :as db]
            [chronograph-web.db.tasks :as tasks-db]))

(rf/reg-sub
  ::invites
  (fn [db _]
    (org-invites-db/invites db)))

(rf/reg-sub
  ::current-organization-timers
  (fn [db [_ date]]
    (->> (timers-db/timers-with-tasks db date (:id (org-context-db/current-organization db)))
         (sort-by :created-at)
         reverse)))
(rf/reg-sub
  ::showing-create-timer-widget?
  (fn [db _]
    (db/get-in-page-state db [:show-create-timer-widget])))

(rf/reg-sub
  ::tasks
  (fn [db _]
    (tasks-db/current-organization-tasks db)))
