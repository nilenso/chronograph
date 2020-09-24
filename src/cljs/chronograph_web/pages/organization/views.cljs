(ns chronograph-web.pages.organization.views
  (:require [re-frame.core :as rf]
            [clojure.spec.alpha :as s]
            [chronograph-web.components.common :as components]
            [chronograph-web.subscriptions :as subs]
            [chronograph-web.pages.organization.events :as org-events]))

(defn- form-valid? [{:keys [name description]}]
  (and
   (s/valid? :tasks/name name)
   (s/valid? :tasks/description description)))

(defn task-form []
  (let [{:keys [form-params status]} @(rf/subscribe [::subs/create-task-form])
        form-invalid? (not (form-valid? form-params))
        {:keys [name description]} form-params]
    [:form
     [components/text-input :name :tasks/name
      {:placeholder "Name"
       :value name
       :on-change #(rf/dispatch [::org-events/create-task-form-update
                                 :name
                                 (.-value (.-currentTarget %))])}]
     [components/text-input :description :tasks/description
      {:placeholder "Description"
       :value description
       :on-change #(rf/dispatch [::org-events/create-task-form-update
                                 :description
                                 (.-value (.-currentTarget %))])}]
     [:button {:type :button
               :name :create
               :disabled (or (= status :creating)
                             form-invalid?)
               :on-click (fn [] (rf/dispatch [::org-events/create-task-form-submit]))}
      (if (= status :creating)
        "Creating..."
        "Create")]]))

(defn task-list-element [{:keys [name]}]
  [:li name])

(defn task-list [organization-id]
  (rf/dispatch [::org-events/fetch-tasks organization-id])
  (fn []
    (let [tasks @(rf/subscribe [::subs/tasks])]
      [:div
       [:h3 "Tasks"]
       [task-form]
       [:ul
        (not-empty (map task-list-element tasks))]])))

(defn organization-page [{:keys [slug]}]
  (rf/dispatch [::org-events/fetch-organization slug])
  (rf/dispatch [::org-events/fetch-tasks slug])
  (fn [_]
    (when-let [{:keys [id name slug created-at updated-at]
                :as organization}
               @(rf/subscribe [::subs/organization slug])]
      (if (= organization ::org-events/not-found)
        [:p "Not found"]

        [:div
         [:strong "Organization details: "]
         [:ul
          [:li "ID: " id]
          [:li "Name: " name]
          [:li "Slug: " slug]
          [:li "Created at: " created-at]
          [:li "Updated at: " updated-at]]
         [task-list id]]))))
