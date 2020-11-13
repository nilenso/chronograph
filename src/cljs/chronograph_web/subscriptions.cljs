(ns chronograph-web.subscriptions
  (:require [re-frame.core :as rf]))

(rf/reg-sub
  ::signin-state
  (fn [db _]
    (get-in db [:user :signin-state])))

(rf/reg-sub
  ::user-info
  (fn [db _]
    (:user db)))

(rf/reg-sub
  ::current-page
  (fn [db _]
    (:page db)))

(rf/reg-sub
  ::page-key
  (fn [db _]
    (:page-key db)))

(rf/reg-sub
  ::organizations
  (fn [db [_ _]]
    (sort-by :slug (vals (get-in db [:organizations])))))

(rf/reg-sub
  ::organization
  (fn [db [_ slug]]
    (get-in db [:organizations slug])))
