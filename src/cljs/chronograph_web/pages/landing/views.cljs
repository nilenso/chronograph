(ns chronograph-web.pages.landing.views
  (:require [re-frame.core :as rf]
            [chronograph-web.events.organization :as org-events]
            [chronograph-web.subscriptions :as subs]))

(defn organizations-list-element [slug organization]
  [:li {:key slug}
   [:a {:href (str "/organizations/" slug)}
    (:name organization)]])

(defn organizations-list [organizations]
  [:div
   (when (not-empty organizations)
     [:div
      [:p "Select organization"]
      [:ul (map #(apply organizations-list-element %) organizations)]])
   [:a {:href "/organization/new"} "New Organization"]])

(defn landing-page [_]
  (fn []
    (rf/dispatch [::org-events/fetch-organizations])
    (let [{:keys [name]} @(rf/subscribe [::subs/user-info])
          organizations @(rf/subscribe [::subs/organizations])]
      [:div
       [:h2 (str "Welcome " name "!")]
       [organizations-list organizations]])))
