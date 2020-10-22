(ns chronograph-web.routes
  (:require [reagent.dom]
            [re-frame.core :as rf]
            [bidi.bidi :as bidi]
            [pushy.core :as pushy]
            [chronograph-web.events.routing :as routing-events]))

(def routes ["/" {"" :root
                  "organizations/" {"new" :organization-new
                                    [:slug] :organization-show}}])

(defn set-page! [match]
  (rf/dispatch [::routing-events/pushy-dispatch match]))

(defonce history (atom nil))

(defn match-route
  [token]
  (or (bidi/match-route routes token)
      (throw (ex-info "Could not match route for token" {:token token}))))

(defn path-for
  [handler & args]
  (apply bidi/path-for routes handler args))

(defn init! []
  (reset! history (pushy/pushy set-page! match-route))
  (pushy/start! @history))
