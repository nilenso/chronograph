(ns chronograph-web.events.organization
  (:require [re-frame.core :as rf]
            [chronograph-web.api-client :as api]
            [chronograph-web.db.organization :as org-db]
            [chronograph-web.config :as config]))

(defn- add-callback-event
  [fx event response]
  (cond-> fx
    event (conj [:dispatch (conj event response)])))

(rf/reg-event-fx
  ::fetch-organizations
  (fn [_ [_ on-success on-failure]]
    {:http-xhrio (api/fetch-organizations [::fetch-organizations-success on-success]
                                          (or on-failure
                                              [::fetch-organizations-fail]))}))

(rf/reg-event-fx
  ::fetch-organizations-success
  (fn [{:keys [db]} [_ on-success organizations]]
    {:db (org-db/set-organizations db organizations)
     :fx (-> []
             (add-callback-event on-success organizations))}))

(rf/reg-event-fx
  ::fetch-organizations-fail
  (fn [_ _]
    {:flash-error {:content (str "Oh no, something went wrong! "
                                 (:frown config/emojis)
                                 " Please refresh the page!")}}))
