(ns chronograph-web.events.routing
  (:require [re-frame.core :as rf]))

(rf/reg-event-db
  ::set-page
  (fn [db [_ route]]
    (assoc db :page route)))

(rf/reg-event-fx
  ::set-token
  (fn [_ [_ token]]
    {:history-token token}))
