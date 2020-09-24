(ns chronograph-web.components.common
  (:require [clojure.spec.alpha :as s]))

(defn text-input [input-name spec {:keys [on-change value] :as attrs}]
  [:div
   [:input
    (merge {:type :text
            :class (when-not (s/valid? spec value) "form-error")
            :name (name input-name)
            :value (or value "")}
           attrs)]])
