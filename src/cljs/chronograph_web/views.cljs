(ns chronograph-web.views
  (:require [re-frame.core :as rf]
            [chronograph-web.subscriptions :as subs]
            [chronograph-web.pages.timers.views :refer [landing-page]]
            [chronograph-web.pages.admin.views :refer [organization-page]]
            [chronograph-web.pages.root.views :refer [root-page]]
            [chronograph-web.components.common :as components]
            [chronograph-web.pages.create-organization.views :refer [create-organization-page]]
            [chronograph-web.pages.welcome.views :refer [welcome-page]]))

(defn login-route []
  (let [location    (-> js/window .-location .-search)
        client-type (-> location (js/URLSearchParams.) (.get "client-type"))]
    (str "/auth/google/login?client-type=" (or client-type "web"))))

(defn- signin-button []
  [:a.google-signin-button-link {:href (login-route)}
   [:span.google-signin-button]])

(defn signin-page []
  [:div
   [:h2 "Please sign in to continue"]
   [signin-button]])

(def authenticated-view {:root             root-page
                         :timers-list      landing-page
                         :admin-page       organization-page
                         :new-organization create-organization-page
                         :welcome-page     welcome-page})

(defn authenticated-page []
  (let [{:keys [route-params] :as page} @(rf/subscribe [::subs/current-page])
        page-key @(rf/subscribe [::subs/page-key])]
    (cond
      (and page-key page) [(authenticated-view page-key) route-params]
      page [components/full-page-spinner]
      :else [:div "Page not found"])))

(defn root []
  (case @(rf/subscribe [::subs/signin-state])
    :signed-in [authenticated-page]
    :signed-out [signin-page]
    :fetching-data [components/full-page-spinner]
    [components/full-page-spinner]))
