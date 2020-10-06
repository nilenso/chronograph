(ns chronograph-web.components.common)

(defn loading-spinner []
  [:h2 "Loading..."])

(defn text-input [input-name {:keys [on-change value] :as attrs}]
  [:div
   [:input
    (merge {:type :text
            :name (name input-name)
            :value (or value "")}
           (assoc attrs :on-change (fn [e]
                                     (on-change (-> e .-currentTarget .-value)))))]])
