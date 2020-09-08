(ns chronograph-web.pages.organization.views
  (:require [re-frame.core :as rf]
            [chronograph-web.subscriptions :as subs]))

(defn organization-page [{:keys [slug]}]
  (if-let [{:keys [name]} @(rf/subscribe [::subs/organization slug])]
    [:div
     [:strong "Name"]
     " "
     name]
    [:p "Not found"]))
