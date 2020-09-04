(ns chronograph-web.views
  (:require [re-frame.core :as rf]
            [chronograph-web.subscriptions :as subs]))

(defn- signin-button []
  [:a.google-signin-button-link {:href  "/google-login"}
   [:span.google-signin-button]])

(defn signin-page []
  [:div
   [:h2 "Please sign in to continue"]
   [signin-button]])

(defn landing-page []
  (let [{:keys [name email photo-url]} @(rf/subscribe [::subs/user-info])]
    [:div
     [:h2 "Welcome!"]
     [:img {:src photo-url}]
     [:p name]
     [:p email]]))

(def authenticated-pages {:root landing-page})

(defn authenticated-page []
  (if-let [{:keys [handler]} @(rf/subscribe [::subs/current-page])]
    [(authenticated-pages handler)]
    [:div "Page not found"]))

(defn loading-page []
  [:h2 "Loading..."])

(defn root []
  (case @(rf/subscribe [::subs/signin-state])
    :signed-in [authenticated-page]
    :signed-out [signin-page]
    :fetching-profile [loading-page]
    [loading-page]))
