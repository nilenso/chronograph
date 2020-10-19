(ns chronograph-web.pages.create-organization.views
  (:require [re-frame.core :as rf]
            [chronograph-web.components.form :as form]
            [chronograph-web.pages.create-organization.events :as events]
            [chronograph-web.http :as http]
            [chronograph-web.subscriptions :as subs]))

(defn create-organization-page [_]
  (let [{::form/keys [get-input-attributes get-submit-attributes]}
        (form/form {:form-key ::create-organization
                    :request-builder (fn [{name :name slug :slug}]
                                       (http/post {:uri "/api/organizations/"
                                                   :params {:name name
                                                            :slug slug}
                                                   :on-success [::events/create-organization-succeeded]
                                                   :on-failure [::events/create-organization-failed]}))})
        page-error (rf/subscribe [::subs/page-errors])]
    (fn [_]
      [:form
       (when (contains? @page-error ::events/error-create-organization-failed)
         [:div "Error creating the organization"])
       [:div [:input (get-input-attributes :name {:type :text :autoFocus true} :organizations/name)]]
       [:div [:input (get-input-attributes :slug {:type :text} :organizations/slug)]]
       [:button (get-submit-attributes) "Create"]])))
