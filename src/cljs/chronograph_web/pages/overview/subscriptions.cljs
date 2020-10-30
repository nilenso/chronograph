(ns chronograph-web.pages.overview.subscriptions
  (:require [chronograph-web.db.organization-invites :as org-invites-db]
            [chronograph-web.db.tasks :as tasks-db]
            [chronograph-web.db.timers :as timers-db]
            [re-frame.core :as rf]))

(rf/reg-sub
  ::invites
  (fn [db _]
    (org-invites-db/invites db)))

(rf/reg-sub
  ::timers
  (fn [db [_ date org-id]]
    (map #(assoc % :task (tasks-db/find-by-id db (:task-id %)))
         (timers-db/timers-by-date db date))))
