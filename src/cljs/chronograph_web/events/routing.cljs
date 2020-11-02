(ns chronograph-web.events.routing
  (:require [re-frame.core :as rf]
            [chronograph-web.db :as db]))

(defmulti on-route-change-event :handler :default ::default)

(defmethod on-route-change-event ::default [_] nil)

(rf/reg-event-fx
  ::pushy-dispatch
  (fn [{:keys [db]} [_ route]]
    {:db (-> db
             (assoc :page route)
             (db/clear-page-state))
     :fx (if-let [event (on-route-change-event route)]
           [[:dispatch [event]]]
           [])}))
