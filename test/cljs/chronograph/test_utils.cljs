(ns chronograph.test-utils
  (:require [re-frame.core :as rf]
            [bidi.bidi :as bidi]
            [chronograph-web.events.routing :as routing-events]
            [chronograph-web.routes :as routes]))

(defn set-token [token]
  (rf/dispatch [::routing-events/pushy-dispatch (bidi/match-route routes/routes token)]))

(defn stub-routing []
  (rf/reg-fx
    :history-token
    set-token))
