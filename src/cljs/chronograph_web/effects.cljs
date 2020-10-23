(ns chronograph-web.effects
  (:require [re-frame.core :as rf]
            [pushy.core :as pushy]
            [chronograph-web.routes :as routes]
            ["antd" :as antd]))

(rf/reg-fx
  :history-token
  (fn [token]
    (pushy/set-token! @routes/history token)))

(defn- flash-error
  "Flashes an error message. Refer to
  https://ant.design/components/message/ for details."
  [content duration-secs]
  (antd/message.error (clj->js {:content content
                                :style   {:whiteSpace "pre"}})
                      duration-secs))

(rf/reg-fx
  :flash-error
  (fn [{:keys [content duration-secs]
        :or   {duration-secs 4}}]
    (flash-error content duration-secs)))
