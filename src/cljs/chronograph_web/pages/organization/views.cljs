(ns chronograph-web.pages.organization.views
  (:require [re-frame.core :as rf]
            [chronograph-web.components.form :as form]
            [chronograph-web.pages.organization.events :as org-events]
            [chronograph-web.pages.organization.subscriptions :as org-subs]
            [chronograph-web.components.common :as components]
            [chronograph-web.subscriptions :as subs]
            [chronograph-web.http :as http]))

(defn- add-members-form []
  (let [slug @(rf/subscribe [::org-subs/org-slug])
        {::form/keys [get-input-attributes get-submit-attributes]}
        (form/form {:form-key ::add-member
                    :request-builder (fn [{:keys [email]}]
                                       (http/post {:uri (str "/api/organizations/"
                                                             slug
                                                             "/members")
                                                   :params {:email email}
                                                   :on-success [::org-events/invite-member-succeeded]
                                                   :on-failure [::org-events/invite-member-failed]}))})]
    (fn []
      [:div
       [:input (get-input-attributes :email)]
       [:button (get-submit-attributes) "Invite"]])))

(defn- tasks-uri [slug]
  (str "/api/organizations/" slug "/tasks/"))

(defn create-task-form [{:keys [slug] :as _organization}]
  (let [{::form/keys [get-input-attributes get-submit-attributes]}
        (form/form {:form-key        ::create-task
                    :request-builder (fn [{:keys [name description]}]
                                       (http/post {:uri        (tasks-uri slug)
                                                   :params     {:name        name
                                                                :description description}
                                                   :on-success [::org-events/fetch-tasks slug]
                                                   :on-failure [::org-events/create-task-failed]}))})]
    (fn [_organization]
      [:form
       [:div [:input (get-input-attributes :name nil :tasks/name)]]
       [:div [:input (get-input-attributes :description nil :tasks/description)]]
       [:button (get-submit-attributes) "Save"]])))

(defn update-form
  [{:keys [id name description] :as task}]
  (let [slug @(rf/subscribe [::org-subs/org-slug])
        {::form/keys [get-input-attributes get-submit-attributes]}
        (form/form {:form-key [::update-task id]
                    :initial-values task
                    :request-builder (fn [task]
                                       (http/put {:uri (str (tasks-uri slug) id)
                                                  :params {:updates task}
                                                  :on-success [::org-events/update-task-success id]
                                                  :on-failure [::org-events/update-task-failure id]}))})
        form-sub (rf/subscribe [::form/form [::update-task id] nil])]
    (fn [_task]
      (let [status (:status @form-sub)]
        [:form
         [:div [:input (get-input-attributes :name nil :tasks/name)]]
         [:div [:input (get-input-attributes :description nil :tasks/description)]]
         [:button (get-submit-attributes) "Save"]
         [:button {:type     :button
                   :name     :cancel
                   :disabled (= status :submitting)
                   :on-click (fn [] (rf/dispatch [::org-events/cancel-update-task-form id]))}
          "Cancel"]]))))

(defn- archived?
  [{:keys [archived-at] :as _task}]
  archived-at)

(defn task-list-element [{:keys [id name description is-updating] :as task}]
  [:li {:key id}
   [:div
    (cond
      (true? is-updating)
      [update-form task]

      (archived? task)
      [:p [:b name] " - " description]

      :else [:div [:p [:b name] " - " description]
             [:button {:on-click #(rf/dispatch [::org-events/archive-task id])}
              "Archive"]
             [:button {:on-click #(rf/dispatch [::org-events/show-update-form task])}
              "Update"]])]])

(defn task-list [tasks]
  [:div
   [:h3 "Tasks"]
   [:ul
    (not-empty (map task-list-element
                    (filter (complement archived?) tasks)))]
   [:h3 "Archived Tasks"]
   [:ul
    (not-empty (map task-list-element
                    (filter archived? tasks)))]])

(defn organization-page [{:keys [slug]}]
  (rf/dispatch [::org-events/fetch-organization slug])
  (rf/dispatch [::org-events/fetch-members slug])
  (rf/dispatch [::org-events/fetch-tasks slug])
  (fn [_]
    (let [errors @(rf/subscribe [::subs/page-errors])
          {:keys [name] :as organization} @(rf/subscribe [::subs/organization slug])]
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
          [create-task-form organization]
          (when (contains? errors ::org-events/error-creating-task-failed)
            [:p "Failed to save the task. Please try again."])
          (when-let [tasks @(rf/subscribe [::org-subs/tasks])]
            [task-list tasks])]]))))
