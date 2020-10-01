(ns chronograph-web.components.common)

(defn text-input [input-name {:keys [on-change value] :as attrs}]
  [:div
   [:input
    (merge {:type :text
            :name (name input-name)
            :value (or value "")}
           attrs)]])
