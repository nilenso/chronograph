(ns chronograph-web.pages.organization.views
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [chronograph-web.components.form :as form]
            [chronograph-web.pages.organization.events :as org-events]
            [chronograph-web.events.tasks :as task-events]
            [chronograph-web.pages.organization.subscriptions :as org-subs]
            [chronograph-web.components.common :as components]
            [chronograph-web.subscriptions :as subs]
            [chronograph-web.http :as http]
            [chronograph-web.page-container.views :as page-container]
            [chronograph-web.components.antd :as antd]))

(defn- invite-member-form []
  (let [{::form/keys [get-input-attributes get-submit-attributes]}
        (form/form {:form-key        ::add-member
                    :request-builder (fn [{:keys [email]}]
                                       (http/post {:uri        (str "/api/organizations/"
                                                                    @(rf/subscribe [::org-subs/org-slug])
                                                                    "/members")
                                                   :params     {:email email}
                                                   :on-success [::org-events/invite-member-succeeded]
                                                   :on-failure [::org-events/invite-member-failed]}))})]
    (fn []
      [antd/space
       [antd/input (get-input-attributes :email)]
       [antd/button (get-submit-attributes) "Invite"]])))

(defn- tasks-uri [slug]
  (str "/api/organizations/" slug "/tasks/"))

(defn create-task-form []
  (let [{::form/keys [get-input-attributes
                      get-submit-attributes]}
        (form/form {:form-key        ::create-task
                    :request-builder (fn [{:keys [name description]}]
                                       (let [slug @(rf/subscribe [::org-subs/org-slug])]
                                         (http/post {:uri        (tasks-uri slug)
                                                     :params     {:name        name
                                                                  :description description}
                                                     :on-success [::task-events/fetch-tasks slug]
                                                     :on-failure [::org-events/create-task-failed]})))})]
    (fn []
      [antd/space {:direction "vertical"}
       [antd/input (get-input-attributes :name {:spec :tasks/name})]
       [antd/input (get-input-attributes :description {:spec :tasks/description})]
       [antd/button (get-submit-attributes) "Save"]])))

(defn update-task-form
  [{:keys [id] :as task}]
  (let [{::form/keys [get-input-attributes get-submit-attributes submitting?-state]}
        (form/form {:form-key        [::update-task id]
                    :initial-values  task
                    :request-builder (fn [task]
                                       (http/put {:uri        (str (tasks-uri @(rf/subscribe [::org-subs/org-slug]))
                                                                   id)
                                                  :params     {:updates task}
                                                  :on-success [::org-events/update-task-success id]
                                                  :on-failure [::org-events/update-task-failure id]}))})]
    (fn [_task]
      [antd/space {:direction "vertical"}
       [antd/input (get-input-attributes :name {:spec :tasks/name})]
       [antd/input (get-input-attributes :description {:spec :tasks/description})]
       [antd/space
        [antd/button (get-submit-attributes) "Save"]
        [antd/button {:disabled @submitting?-state
                      :on-click #(rf/dispatch [::org-events/hide-update-task-form id])}
         "Cancel"]]])))

(defn task-list-element [{:keys [id name description] :as task}]
  (if @(rf/subscribe [::org-subs/show-update-task-form? id])
    [update-task-form task]
    [antd/space {:direction "vertical"}
     [antd/text {:strong true} name]
     [antd/text description]]))

(defn archive-button [{:keys [id]}]
  (when (and @(rf/subscribe [::org-subs/user-is-admin?])
             (not @(rf/subscribe [::org-subs/show-update-task-form? id])))
    [antd/button {:on-click #(rf/dispatch [::org-events/archive-task id])
                  :type     "link"
                  :danger   true}
     "Archive"]))

(defn update-button [{:keys [id]}]
  (when (and @(rf/subscribe [::org-subs/user-is-admin?])
             (not @(rf/subscribe [::org-subs/show-update-task-form? id])))
    [antd/button {:on-click #(rf/dispatch [::org-events/show-update-task-form id])
                  :type     "link"}
     "Update"]))

(defn tasks-list []
  (r/with-let [admin? (rf/subscribe [::org-subs/user-is-admin?])]
    [antd/list {:dataSource @(rf/subscribe [::org-subs/tasks])
                :renderItem (fn [task]
                              (antd/list-item {:actions [[archive-button task]
                                                         [update-button task]]}
                                              [task-list-element task]))
                :header     [antd/title {:level 4
                                         :style {:margin-bottom "0px"}}
                             "Tasks"]
                :footer     (when @admin?
                              [create-task-form])}]))

(defn organization-page [{:keys [slug]}]
  (let [{:keys [name]} @(rf/subscribe [::subs/organization slug])]
    (if-not name
      [components/loading-spinner]
      [page-container/org-scoped-page-container
       [antd/page-header

        (when @(rf/subscribe [::org-subs/user-is-admin?])
          [antd/list {:dataSource (concat (for [member @(rf/subscribe [::org-subs/joined-members])]
                                            (:name member))
                                          (for [member @(rf/subscribe [::org-subs/invited-members])]
                                            (str (:email member) " (invited)")))
                      :renderItem (fn [text]
                                    (antd/list-item text))
                      :header     [antd/space {:align "center"}
                                   [antd/title {:level 4
                                                :style {:margin-bottom "0px"}}
                                    "Members"]
                                   (when @(rf/subscribe [::org-subs/user-is-admin?])
                                     [invite-member-form])]}])
        [tasks-list]]])))
