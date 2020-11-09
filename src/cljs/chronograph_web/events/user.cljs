(ns chronograph-web.events.user
  (:require [re-frame.core :as rf]
            [chronograph-web.db :as db]
            [day8.re-frame.http-fx]
            [chronograph-web.db.organization :as org-db]
            [chronograph-web.api-client :as api]
            [chronograph-web.routes :as routes]))

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
    (if (not-empty (org-db/organizations db))
      {:db (sign-in-user db response)}
      {:fx [[:http-xhrio (api/fetch-organizations [::fetch-organizations-succeeded response]
                                                  [::fetch-data-failed])]]})))

(rf/reg-event-fx
  ::fetch-organizations-succeeded
  (fn [{:keys [db]} [_ user-response response]]
    {:db (-> db
             (org-db/add-organizations response)
             (sign-in-user user-response))
     :fx [[:history-token (routes/path-for :timers-list :slug (:slug (first response)))]]}))

(rf/reg-event-db
  ::fetch-data-failed
  (fn [db _]
    (assoc-in db [:user :signin-state] :signed-out)))
