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
  ::create-organization-form
  (fn [db _]
    (get-in db [:create-organization])))

(rf/reg-sub
  ::organizations
  (fn [db [_ _]]
    (get-in db [:organizations])))

(rf/reg-sub
  ::organization
  (fn [db [_ slug]]
    (get-in db [:organizations slug])))

(rf/reg-sub
  ::tasks
  (fn [db [_ _]]
    (get-in db [:tasks])))

(rf/reg-sub
  ::create-task-form
  (fn [db _]
    (get-in db [:create-task])))
