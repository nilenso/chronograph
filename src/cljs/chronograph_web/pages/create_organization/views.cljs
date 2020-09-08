(ns chronograph-web.pages.create-organization.views
  (:require [re-frame.core :as rf]
            [chronograph-web.pages.create-organization.events :as events]
            [chronograph-web.subscriptions :as subs]))

(defn- text-input [{:keys [on-change value] :as m}]
  [:input (merge {:type :text}
                 m
                 {:value (or value "")
                  :on-change #(on-change (.-value (.-currentTarget %)))})])

(defn create-organization-page [_]
  (let [{:keys [form-params status] :as xyz} @(rf/subscribe [::subs/create-organization-form])
        {:keys [name slug]} form-params]
    [:form
     (if (= status :failed)
      [:div "Error creating the organization"])
     (text-input {:type :text
                  :name "name"
                  :placeholder "Name"
                  :value name
                  :on-change #(rf/dispatch [::events/create-organization-form-update :name %])})
     (text-input {:type :text
                  :name "slug"
                  :placeholder "Slug"
                  :value slug
                  :on-change #(rf/dispatch [::events/create-organization-form-update :slug %])})
     [:button {:type :button
               :name :create
               :disabled (= status :creating)
               :on-click (fn [] (rf/dispatch [::events/create-organization-form-submit]))}
      (if (= status :creating)
        "Creating..."
        "Create")]]))
