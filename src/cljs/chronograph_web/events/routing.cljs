(ns chronograph-web.events.routing
  (:require [re-frame.core :as rf]
            [chronograph-web.db :as db]))

(rf/reg-event-db
  ::pushy-dispatch
  (fn [db [_ route]]
    (-> db
        (assoc :page route)
        (db/clear-page-state))))
