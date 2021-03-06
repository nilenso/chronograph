(ns chronograph-web.events.routing
  (:require [re-frame.core :as rf]
            [chronograph-web.db :as db]))

(defmulti on-route-change-event :handler :default ::default)

(defmethod on-route-change-event ::default [_] nil)

(defmulti on-pre-route-change-event :handler :default ::default)

(defmethod on-pre-route-change-event
  ::default
  [_route {:keys [db]}]
  {:db (db/clear-page-state db)})

(rf/reg-event-fx
  ::pre-handle-change
  (fn [fx [_ route]]
    (on-pre-route-change-event route fx)))

(rf/reg-event-fx
  ::handle-change
  (fn [{:keys [db]} [_ route]]
    (if-let [event (on-route-change-event route)]
      {:fx [[:dispatch event]]}
      {})))

(rf/reg-event-fx
  ::pushy-dispatch
  (fn [{:keys [db]} [_ route]]
    {:db (-> db
             (assoc :page route))
     :fx [[:dispatch [::pre-handle-change route]]
          [:dispatch [::handle-change route]]]}))
