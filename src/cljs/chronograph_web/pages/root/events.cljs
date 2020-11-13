(ns chronograph-web.pages.root.events
  (:require [chronograph-web.events.routing :as routing-events]
            [re-frame.core :as rf]
            [chronograph-web.api-client :as api]
            [chronograph-web.db.organization :as org-db]
            [chronograph-web.routes :as routes]
            [chronograph-web.config :as config]))

(defmethod routing-events/on-route-change-event
  :root
  [_]
  [::root-page-navigated])

(rf/reg-event-fx
  ::root-page-navigated
  (fn [_ _]
    {:fx [[:http-xhrio (api/fetch-organizations [::fetch-organizations-succeeded]
                                                [::fetch-organizations-failed])]]}))

(rf/reg-event-fx
  ::fetch-organizations-succeeded
  (fn [{:keys [db]} [_ response]]
    {:db (org-db/add-organizations db response)
     :fx [[:history-token (routes/path-for :timers-list :slug (:slug (first response)))]]}))

(rf/reg-event-fx
  ::fetch-organizations-failed
  (fn [_ _]
    {:flash-error {:content (str "Oh no, something went wrong! "
                                 (:frown config/emojis)
                                 " Please refresh the page!")}}))
