(ns chronograph-web.effects
  (:require [re-frame.core :as rf]
            [pushy.core :as pushy]
            [chronograph-web.routes :as routes]))

(rf/reg-fx
  :history-token
  (fn [token]
    (pushy/set-token! @routes/history token)))
