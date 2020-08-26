(ns chronograph-web.subscriptions
  (:require [re-frame.core :as rf]))

(rf/reg-sub
  ::google-client-initialized?
  (fn [db _]
    (:google-client-initialized? db)))

(rf/reg-sub
  ::signed-in?
  (fn [db _]
    (:signed-in? db)))

(rf/reg-sub
  ::signin-state
  (fn [db _]
    (get-in db [:user :signin-state])))

(rf/reg-sub
  ::user-info
  (fn [db _]
    (:user db)))
