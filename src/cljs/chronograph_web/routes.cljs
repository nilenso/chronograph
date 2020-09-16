(ns chronograph-web.routes
  (:require [reagent.dom]
            [re-frame.core :as rf]
            [bidi.bidi :as bidi]
            [pushy.core :as pushy]
            [chronograph-web.pages.landing.views :refer [landing-page]]
            [chronograph-web.pages.create-task.views :refer [create-task-page]]
            [chronograph-web.pages.create-organization.views :refer [create-organization-page]]
            [chronograph-web.pages.organization.views :refer [organization-page]]
            [chronograph-web.events.routing :as routing-events]))

(def routes ["/" {"" :root
                  "organization/" {"new" :organization-new
                                   [:slug] :organization-show}
                  "tasks/" {"new" :tasks-new}}])

(def authenticated-view {:root landing-page
                         :organization-new create-organization-page
                         :organization-show organization-page
                         :tasks-new create-task-page})

(defn set-page! [match]
  (rf/dispatch [::routing-events/set-page match]))

(def history
  (pushy/pushy set-page! #(bidi/match-route routes %)))

(defn init! []
  (pushy/start! history))
