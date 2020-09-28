(ns chronograph-web.pages.landing.views
  (:require [re-frame.core :as rf]
            [chronograph-web.subscriptions :as subs]))

(defn landing-page [_]
  (let [{:keys [name email photo-url]} @(rf/subscribe [::subs/user-info])]
    [:div
     [:a {:href "/pending-invites"} "You have pending invites!"]
     [:a {:href "/organization/new"} "New Organization"]
     [:h2 "Welcome!"]
     [:img {:src photo-url}]
     [:p name]
     [:p email]]))
