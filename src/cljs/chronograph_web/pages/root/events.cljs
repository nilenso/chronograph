(ns chronograph-web.pages.root.events
  (:require [chronograph-web.events.routing :as routing-events]
            [chronograph-web.events.organization :as org-events]
            [chronograph-web.db :as db]
            [re-frame.core :as rf]
            [chronograph-web.routes :as routes]))

(defmethod routing-events/on-route-change-event
  :root
  [_]
  [::root-page-navigated])

(rf/reg-event-fx
  ::root-page-navigated
  (fn [{:keys [db]} _]
    {:db (db/set-page-key db :root)
     :fx [[:dispatch [::org-events/fetch-organizations [::after-organizations-fetched]]]]}))

(rf/reg-event-fx
  ::after-organizations-fetched
  (fn [_ [_ response]]
    (if (> (count response) 0)
      {:fx [[:history-token (routes/path-for :timers-list :slug (:slug (first response)))]]}
      {:fx [[:history-token (routes/path-for :welcome-page)]]})))
