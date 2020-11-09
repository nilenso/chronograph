(ns chronograph-web.pages.organization.views
  (:require [re-frame.core :as rf]
            [chronograph-web.components.form :as form]
            [chronograph-web.pages.organization.events :as org-events]
            [chronograph-web.pages.organization.subscriptions :as org-subs]
            [chronograph-web.components.common :as components]
            [chronograph-web.subscriptions :as subs]
            [chronograph-web.http :as http]
            [chronograph-web.page-container.views :as page-container]))

(defn- invite-member-form []
  (let [slug @(rf/subscribe [::org-subs/org-slug])
        {::form/keys [get-input-attributes get-submit-attributes]}
        (form/form {:form-key        ::add-member
                    :request-builder (fn [{:keys [email]}]
                                       (http/post {:uri        (str "/api/organizations/"
                                                                    slug
                                                                    "/members")
                                                   :params     {:email email}
                                                   :on-success [::org-events/invite-member-succeeded]
                                                   :on-failure [::org-events/invite-member-failed]}))})]
    (fn []
      [:div
       [:h2 "Add Members"]
       [:div
        [:input (get-input-attributes :email)]
        [:button (get-submit-attributes) "Invite"]]])))

(defn- tasks-uri [slug]
  (str "/api/organizations/" slug "/tasks/"))

(defn create-task-form []
  (let [{::form/keys [get-input-attributes get-submit-attributes]}
        (form/form {:form-key        ::create-task
                    :request-builder (fn [{:keys [name description]}]
                                       (let [slug @(rf/subscribe [::org-subs/org-slug])]
                                         (http/post {:uri        (tasks-uri slug)
                                                     :params     {:name        name
                                                                  :description description}
                                                     :on-success [::org-events/fetch-tasks slug]
                                                     :on-failure [::org-events/create-task-failed]})))})]
    (fn []
      [:form
       [:div [:input (get-input-attributes :name {:spec :tasks/name})]]
       [:div [:input (get-input-attributes :description {:spec :tasks/description})]]
       [:button (get-submit-attributes) "Save"]])))

(defn update-task-form
  [{:keys [id] :as task}]
  (let [slug @(rf/subscribe [::org-subs/org-slug])
        {::form/keys [get-input-attributes get-submit-attributes submitting?-state]}
        (form/form {:form-key        [::update-task id]
                    :initial-values  task
                    :request-builder (fn [task]
                                       (http/put {:uri        (str (tasks-uri slug) id)
                                                  :params     {:updates task}
                                                  :on-success [::org-events/update-task-success id]
                                                  :on-failure [::org-events/update-task-failure id]}))})]
    (fn [_task]
      [:form
       [:div [:input (get-input-attributes :name {:spec :tasks/name})]]
       [:div [:input (get-input-attributes :description {:spec :tasks/description})]]
       [:button (get-submit-attributes) "Save"]
       [:button {:type     :button
                 :name     :cancel
                 :disabled @submitting?-state
                 :on-click #(rf/dispatch [::org-events/hide-update-task-form id])}
        "Cancel"]])))

(defn- archived?
  [{:keys [archived-at] :as _task}]
  archived-at)

(defn task-list-element [{:keys [id name description] :as task}]
  [:li {:key id}
   [:div
    (if @(rf/subscribe [::org-subs/show-update-task-form? id])
      [update-task-form task]
      [:div [:p [:b name] " - " description]
       (when @(rf/subscribe [::org-subs/user-is-admin?])
         [:<>
          [:button {:on-click #(rf/dispatch [::org-events/archive-task id])}
           "Archive"]
          [:button {:on-click #(rf/dispatch [::org-events/show-update-task-form id])}
           "Update"]])])]])

(defn task-list [tasks]
  [:div
   [:ul
    (->> tasks
         (filter (complement archived?))
         (map task-list-element)
         (doall)
         (not-empty))]])

(defn tasks-section []
  [:div
   [:h2 "Tasks"]
   (when @(rf/subscribe [::org-subs/user-is-admin?])
     [create-task-form])
   (when-let [tasks @(rf/subscribe [::org-subs/tasks])]
     [task-list tasks])])

(defn organization-page [{:keys [slug]}]
  (let [{:keys [name]} @(rf/subscribe [::subs/organization slug])]
    (if-not name
      [components/loading-spinner]
      [page-container/org-scoped-page-container
       [:div
        [:h1 name]
        (when @(rf/subscribe [::org-subs/user-is-admin?])
          [invite-member-form])
        (when @(rf/subscribe [::org-subs/user-is-admin?])
          [:div
           [:h2 "Members"]
           [:ul
            (for [member @(rf/subscribe [::org-subs/joined-members])]
              ^{:key (str "joined-" (:id member))} [:li (:name member)])
            (for [member @(rf/subscribe [::org-subs/invited-members])]
              ^{:key (str "invited-" (:email member))} [:li (str (:email member) " (invited)")])]])
        [tasks-section]]])))
