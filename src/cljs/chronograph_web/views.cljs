(ns chronograph-web.views
  (:require [re-frame.core :as rf]
            [chronograph-web.subscriptions :as subs]))

(defn- signin-button []
  [:div
   [:h2 "Please sign in to continue"]
   [:a {:href "/google-login"}
    "Sign in with Google"]])

(defn signin-page []
  [signin-button])

(defn landing-page []
  (let [{:keys [name email]} @(rf/subscribe [::subs/user-info])]
    [:div
     [:h2 "Welcome!"]
     [:p name]
     [:p email]]))

(defn loading-page []
  [:h2 "Loading..."])

(defn root []
  (case @(rf/subscribe [::subs/signin-state])
    :signed-in [landing-page]
    :signed-out [signin-page]
    :fetching-profile [loading-page]
    [loading-page]))
