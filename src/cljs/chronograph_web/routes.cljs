(ns chronograph-web.routes
  (:require [reagent.dom]
            [re-frame.core :as rf]
            [bidi.bidi :as bidi]
            [pushy.core :as pushy]
            [chronograph-web.pages.landing.views :refer [landing-page]]
            [chronograph-web.pages.create-organization.views :refer [create-organization-page]]
            [chronograph-web.pages.organization.views :refer [organization-page]]
            [chronograph-web.events.routing :as routing-events]))

(def routes ["/" {"" :root
                  "organizations/" {"new" :organization-new
                                    [:slug] :organization-show}}])

(def authenticated-view {:root landing-page
                         :organization-new create-organization-page
                         :organization-show organization-page})

(defn set-page! [match]
  (rf/dispatch [::routing-events/pushy-dispatch match]))

(defonce history (atom nil))

(defn match-route
  [token]
  (or (bidi/match-route routes token)
      (throw (ex-info "Could not match route for token" {:token token}))))

(defn init! []
  (reset! history (pushy/pushy set-page! match-route))
  (pushy/start! @history))
