(ns chronograph-web.events.routing
  (:require [re-frame.core :as rf]))

(rf/reg-event-fx
  ::set-token
  (fn [_ [_ token]]
    {:history-token token}))
