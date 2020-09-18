(ns chronograph-web.views.components)

(defn loading-spinner []
  [:h2 "Loading..."])

(defn input [{:keys [on-change] :as attrs}]
  [:input (assoc attrs :on-change (fn [e]
                                    (on-change (-> e .-currentTarget .-value))))])
