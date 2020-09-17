(ns chronograph-web.pages.organization.views
  (:require [re-frame.core :as rf]
            [chronograph-web.subscriptions :as subs]
            [chronograph-web.pages.organization.events :as org-events]))

(defn organization-page [{:keys [slug]}]
  (rf/dispatch [::org-events/fetch-organization slug])
  (fn [_]
    (if-let [{:keys [id name slug created-at updated-at]}
             @(rf/subscribe [::subs/organization slug])]
      [:div
       [:strong "Organization details: "]
       [:ul
        [:li "ID: " id]
        [:li "Name: " name]
        [:li "Slug: " slug]
        [:li "Created at: " created-at]
        [:li "Updated at: " updated-at]]]
      [:p "Not found"])))
