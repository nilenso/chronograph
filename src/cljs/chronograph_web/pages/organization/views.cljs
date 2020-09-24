(ns chronograph-web.pages.organization.views
  (:require [re-frame.core :as rf]
            [chronograph-web.pages.organization.events :as org-events]
            [chronograph-web.pages.organization.subscriptions :as org-subs]
            [chronograph-web.subscriptions :as subs]
            [chronograph-web.views.components :as c]))

(defn- add-members-form []
  [:div
   [c/input {:placeholder "E-mail"
             :on-change   #(rf/dispatch [::org-events/email-input-changed %])
             :value       @(rf/subscribe [::org-subs/email-input-value])}]
   [:button {:on-click #(rf/dispatch [::org-events/invite-button-clicked])}
    "Invite"]])

(defn organization-page [{:keys [slug]}]
  (rf/dispatch [::org-events/page-mounted])
  (fn [_]
    (let [errors @(rf/subscribe [::org-subs/page-errors])
          {:keys [name]} @(rf/subscribe [::subs/organization slug])]
      (cond
        (contains? errors ::org-events/error-org-not-found) [:h2 "Not found"]
        (not name) [c/loading-spinner]
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
               ^{:key (str "invited-" (:email member))} [:li (str (:email member) " (invited)")])])]]))))
