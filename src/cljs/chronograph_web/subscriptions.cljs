(ns chronograph-web.subscriptions
  (:require [re-frame.core :as rf]
            [chronograph-web.db :as db]))

(rf/reg-sub
  ::page-errors
  (fn [db _]
    (db/get-errors db)))

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
    (vals (get-in db [:tasks]))))

(rf/reg-sub
  ::create-task-form
  (fn [db _]
    (get-in db [:create-task])))

(rf/reg-sub
  ::update-task-form
  (fn [db [_ task-id]]
    (get-in db [:update-task task-id])))
