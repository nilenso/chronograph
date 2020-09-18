(ns chronograph-web.pages.create-organization.views
  (:require [re-frame.core :as rf]
            [chronograph-web.pages.create-organization.events :as create-events]
            [chronograph-web.pages.create-organization.subscriptions :as create-subs]
            [clojure.spec.alpha :as s]))

(defn- form-valid? [{:keys [name slug]}]
  (and
   (s/valid? :organizations/name name)
   (s/valid? :organizations/slug slug)))

(defn- text-input [k spec {:keys [on-change value] :as attrs}]
  [:div
   [:input (merge {:type :text
                   :on-change #(rf/dispatch [::create-events/create-organization-form-update
                                             k
                                             (.-value (.-currentTarget %))])}
                  attrs
                  {:value (or value "")
                   :class (when (not (s/valid? spec value)) "form-error")
                   :name (name k)})]])

(defn create-organization-page [_]
  (let [{:keys [form-params status] :as _form-data} @(rf/subscribe [::create-subs/create-organization-form])
        form-invalid? (not (form-valid? form-params))
        {:keys [name slug]} form-params]
    [:form
     (when (= status :failed)
       [:div "Error creating the organization"])
     (when (and status form-invalid?)
       [:div "Please fix form errors"])
     (text-input :name
                 :organizations/name
                 {:placeholder "Name" :value name :autoFocus true})
     (text-input :slug
                 :organizations/slug
                 {:placeholder "Slug" :value slug})
     [:button {:type :button
               :name :create
               :disabled (or (= status :creating)
                             form-invalid?)
               :on-click (fn [] (rf/dispatch [::create-events/create-organization-form-submit]))}
      (if (= status :creating)
        "Creating..."
        "Create")]]))
