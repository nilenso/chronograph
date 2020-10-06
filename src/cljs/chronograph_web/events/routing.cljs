(ns chronograph-web.events.routing
  (:require [re-frame.core :as rf]))

(rf/reg-event-db
  ::pushy-dispatch
  (fn [db [_ route]]
    (assoc db :page route)))
