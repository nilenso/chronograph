(ns chronograph-web.pages.organization.views
  (:require [re-frame.core :as rf]
            [chronograph-web.pages.organization.events :as org-events]
            [chronograph-web.pages.organization.subscriptions :as org-subs]
            [chronograph-web.components.common :as components]
            [chronograph-web.subscriptions :as subs]))

(defn- add-members-form []
  [:div
   [components/text-input :email
    {:placeholder "E-mail"
     :value       @(rf/subscribe [::org-subs/email-input-value])
     :on-change   #(rf/dispatch [::org-events/email-input-changed %])}]
   [:button {:on-click #(rf/dispatch [::org-events/invite-button-clicked])}
    "Invite"]])

(defn task-form []
  (let [{:keys [form-params status]} @(rf/subscribe [::subs/create-task-form])
        {:keys [name description]} form-params]
    [:form
     [components/text-input :name
      {:placeholder "Name"
       :value       name
       :on-change   #(rf/dispatch [::org-events/create-task-form-update
                                   :name
                                   %])}]
     [components/text-input :description
      {:placeholder "Description"
       :value       description
       :on-change   #(rf/dispatch [::org-events/create-task-form-update
                                   :description
                                   %])}]
     [:button {:type :button
               :name :create
               :disabled (or (= status :creating))
               :on-click (fn [] (rf/dispatch [::org-events/create-task-form-submit]))}
      (if (= status :creating)
        "Creating..."
        "Create")]]))

(defn update-form [{:keys [id] :as _task}]
  (let [{:keys [form-params status]} @(rf/subscribe [::subs/update-task-form id])
        {:keys [name description]} form-params]
    [:form
     [components/text-input :name
      {:placeholder "Name"
       :value       name
       :on-change   #(rf/dispatch [::org-events/update-task-form-update
                                   id
                                   :name
                                   %])}]
     [components/text-input :description
      {:placeholder "Description"
       :value       description
       :on-change   #(rf/dispatch [::org-events/update-task-form-update
                                   id
                                   :description
                                   %])}]
     [:button {:type :button
               :name :save
               :disabled (or (= status :saving))
               :on-click (fn [] (rf/dispatch [::org-events/update-task-form-submit id]))}
      (if (= status :saving)
        "Saving..."
        "Save")]
     [:button {:type :button
               :name :save
               :disabled (or (= status :saving))
               :on-click (fn [] (rf/dispatch [::org-events/cancel-update-task-form id]))}
      "Cancel"]]))

(defn task-list-element [{:keys [id name description is-updating] :as task}]
  [:li {:key id}
   [:div
    (if (true? is-updating)
      [update-form task]
      [:p [:b name] " - " description])
    [:button {:on-click #(rf/dispatch [::org-events/archive-task id])}
     "Archive"]
    [:button {:on-click #(rf/dispatch [::org-events/show-update-form task])}
     "Update"]]])

(defn task-list [tasks]
  [:div
   [:h3 "Tasks"]
   [:ul
    (not-empty (map task-list-element tasks))]])

(defn organization-page [{:keys [slug]}]
  (rf/dispatch [::org-events/fetch-organization slug])
  (rf/dispatch [::org-events/fetch-members slug])
  (rf/dispatch [::org-events/fetch-tasks slug])
  (fn [_]
    (let [errors @(rf/subscribe [::org-subs/page-errors])
          {:keys [name]} @(rf/subscribe [::subs/organization slug])]
      (cond
        (contains? errors ::org-events/error-org-not-found) [:h2 "Not found"]
        (not name) [components/loading-spinner]
        :else
        [:div
         [:h1 name]
         [:div
          [:h2 "Add Members"]
          [add-members-form]
          (when (contains? errors ::org-events/error-invite-member-failed)
            [:p "Failed to invite the member. Please try again."])]
         [:div
          [:h2 "Members"]
          (if (contains? errors ::org-events/error-fetch-members-failed)
            [:p "Failed to load members of the organization. Please refresh the page."]
            [:ul
             (for [member @(rf/subscribe [::org-subs/joined-members])]
               ^{:key (str "joined-" (:id member))} [:li (:name member)])
             (for [member @(rf/subscribe [::org-subs/invited-members])]
               ^{:key (str "invited-" (:email member))} [:li (str (:email member) " (invited)")])])]
         [:div
          [:h2 "Tasks"]
          [task-form]
          (when-let [tasks @(rf/subscribe [::subs/tasks])]
            [task-list tasks])]]))))
