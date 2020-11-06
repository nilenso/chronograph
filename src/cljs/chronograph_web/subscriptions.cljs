(ns chronograph-web.subscriptions
  (:require [re-frame.core :as rf]
            [chronograph-web.db.tasks :as tasks-db]
            [chronograph-web.db.timers :as timers-db]))

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
    (sort-by :slug (vals (get-in db [:organizations])))))

(rf/reg-sub
  ::organization
  (fn [db [_ slug]]
    (get-in db [:organizations slug])))

(rf/reg-sub
 ::timers
 (fn [db [_ date organization-id]]
     (->> (timers-db/timers-by-date db date)
          (map #(assoc % :task (tasks-db/find-by-id db (:task-id %))))
          (filter #(= organization-id (get-in % [:task :organization-id]))))))
