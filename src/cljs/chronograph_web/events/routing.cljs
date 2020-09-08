(ns chronograph-web.events.routing
  (:require [re-frame.core :as rf]
            [chronograph-web.db :as db]))

(rf/reg-event-db
  ::set-page
  (fn [db [_ route]]
    (assoc db :page route)))
