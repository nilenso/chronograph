(ns chronograph-web.views
  (:require [re-frame.core :as rf]
            [chronograph-web.events :as events]
            [chronograph-web.routes :as routes]
            [chronograph-web.subscriptions :as subs]))

(defn- signin-button []
  [:a.google-signin-button-link {:href  "/google-login"}
   [:span.google-signin-button]])

(defn signin-page []
  [:div
   [:h2 "Please sign in to continue"]
   [signin-button]])

(defn authenticated-page []
  (if-let [{:keys [handler route-params] :as route} @(rf/subscribe [::subs/current-page])]
    ((routes/authenticated-view handler) route-params)
    [:div "Page not found"]))

(defn loading-page []
  [:h2 "Loading..."])

(defn root []
  (case @(rf/subscribe [::subs/signin-state])
    :signed-in [authenticated-page]
    :signed-out [signin-page]
    :fetching-profile [loading-page]
    [loading-page]))
