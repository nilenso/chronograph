(ns chronograph-web.events.user
  (:require [re-frame.core :as rf]
            [chronograph-web.db :as db]
            [day8.re-frame.http-fx]
            [chronograph-web.api-client :as api]))

(rf/reg-event-fx
  ::initialize
  (fn [_ _]
    {:db (assoc-in db/default-db [:user :signin-state] :fetching-data)
     :fx [[:http-xhrio (api/fetch-profile [::fetch-profile-succeeded]
                                          [::fetch-data-failed])]]}))

(defn- sign-in-user
  [db user-response]
  (assoc db :user (merge {:signin-state :signed-in} user-response)))

(rf/reg-event-fx
  ::fetch-profile-succeeded
  (fn [{:keys [db]} [_ response]]
    {:db (sign-in-user db response)}))

(rf/reg-event-db
  ::fetch-data-failed
  (fn [db _]
    (assoc-in db [:user :signin-state] :signed-out)))
