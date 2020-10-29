(ns chronograph-web.views
  (:require [re-frame.core :as rf]
            [chronograph-web.subscriptions :as subs]
            [chronograph-web.pages.landing.views :refer [landing-page]]
            [chronograph-web.pages.organization.views :refer [organization-page]]
            [chronograph-web.components.common :as components]
            [chronograph-web.pages :as pages]))

(defn login-route []
  (let [location (-> js/window .-location .-search)
        client-type (-> location (js/URLSearchParams.) (.get "client-type"))]
    (str "/auth/google/login?client-type=" (or client-type "web"))))

(defn- signin-button []
  [:a.google-signin-button-link {:href (login-route)}
   [:span.google-signin-button]])

(defn signin-page []
  [:div
   [:h2 "Please sign in to continue"]
   [signin-button]])

(def authenticated-view {:root              landing-page
                         :organization-show organization-page})

(defn authenticated-page []
  (if-let [{:keys [handler route-params]} @(rf/subscribe [::subs/current-page])]
    [pages/main-page-container
     [(authenticated-view handler) route-params]]
    [:div "Page not found"]))

(defn root []
  (case @(rf/subscribe [::subs/signin-state])
    :signed-in [authenticated-page]
    :signed-out [signin-page]
    :fetching-profile [components/loading-spinner]
    [components/loading-spinner]))
