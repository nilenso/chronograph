(ns chronograph-web.events.routing
  (:require [re-frame.core :as rf]
            [chronograph-web.db :as db]))

(rf/reg-event-db
  ::set-page
  (fn [db [_ route]]
    (assoc db :page route)))

(rf/reg-event-fx
  ::set-token
  (fn [_ [_ token]]
    {:history-token token}))
