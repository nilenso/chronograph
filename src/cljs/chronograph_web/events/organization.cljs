(ns chronograph-web.events.organization
  (:require [re-frame.core :as rf]
            [chronograph-web.api-client :as api]
            [chronograph-web.db.organization :as org-db]
            [chronograph-web.config :as config]
            [chronograph-web.utils.fetch-events :as fetch-events]))

(rf/reg-event-fx
  ::fetch-organizations
  (fetch-events/http-request-creator
   (fn [_ _]
     {:http-xhrio (api/fetch-organizations [::fetch-organizations-success]
                                           [::fetch-organizations-fail])})))

(rf/reg-event-fx
  ::fetch-organizations-success
  (fetch-events/http-success-handler
   (fn [{:keys [db]} [_ organizations]]
     {:db (org-db/set-organizations db organizations)})))

(rf/reg-event-fx
  ::fetch-organizations-fail
  (fn [_ _]
    {:flash-error {:content (str "Oh no, something went wrong! "
                                 (:frown config/emojis)
                                 " Please refresh the page!")}}))
