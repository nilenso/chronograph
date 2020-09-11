(ns chronograph-web.pages.landing.views
  (:require [re-frame.core :as rf]
            [chronograph-web.events :as events]
            [chronograph-web.subscriptions :as subs]))

(defn landing-page [_]
  (let [{:keys [name email photo-url]} @(rf/subscribe [::subs/user-info])]
    [:div
     [:a {:href "/organization/new"} "New Organization"]
     [:h2 "Welcome!"]
     [:img {:src photo-url}]
     [:p name]
     [:p email]]))