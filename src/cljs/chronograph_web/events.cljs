(ns chronograph-web.events
  (:require [re-frame.core :as rf]
            [chronograph-web.db :as db]
            [ajax.core :as ajax]
            [day8.re-frame.http-fx]))

(rf/reg-event-fx
  ::initialize
  (fn [_ _]
    {:db (assoc-in db/default-db [:user :signin-state] :fetching-profile)
     :http-xhrio {:method :get
                  :uri "/api/users/me"
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success [::fetch-profile-succeeded]
                  :on-failure [::fetch-profile-failed]}}))

(rf/reg-event-db
  ::set-page
  (fn [db [_ route]]
    (assoc db :page route)))

(rf/reg-event-db
  ::fetch-profile-succeeded
  (fn [db [_ response]]
    (assoc db :user (merge {:signin-state :signed-in} response))))

(rf/reg-event-db
  ::fetch-profile-failed
  (fn [db [_ response]]
    (assoc-in db [:user :signin-state] :signed-out)))
