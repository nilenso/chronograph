(ns chronograph-web.pages.timers.subscriptions
  (:require [chronograph-web.db.organization-invites :as org-invites-db]
            [re-frame.core :as rf]
            [chronograph-web.db.timers :as timers-db]
            [chronograph-web.db.organization :as org-db]))

(rf/reg-sub
  ::invites
  (fn [db _]
    (org-invites-db/invites db)))

(defn- slug
  [db]
  (get-in db [:page :route-params :slug]))

(rf/reg-sub
  ::current-organization-timers
  (fn [db [_ date]]
    (timers-db/timers-with-tasks db date (org-db/org-id db
                                                        (slug db)))))
