(ns chronograph-web.pages.admin.views
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [chronograph-web.components.form :as form]
            [chronograph-web.pages.admin.events :as admin-events]
            [chronograph-web.events.tasks :as task-events]
            [chronograph-web.pages.admin.subscriptions :as admin-subs]
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
                                                                    @(rf/subscribe [::admin-subs/org-slug])
                                                                    "/members")
                                                   :params     {:email email}
                                                   :on-success [::admin-events/invite-member-succeeded]
                                                   :on-failure [::admin-events/invite-member-failed]}))})]
    (fn []
      [antd/space
       [antd/input (get-input-attributes :email)]
       [antd/button (get-submit-attributes) "Invite"]])))

(defn- tasks-uri [slug]
  (str "/api/organizations/" slug "/tasks/"))

(defn create-task-form [alignment]
  (let [{::form/keys [get-input-attributes
                      get-submit-attributes]}
        (form/form {:form-key        ::create-task
                    :request-builder (fn [{:keys [name description]}]
                                       (let [slug @(rf/subscribe [::admin-subs/org-slug])]
                                         (http/post {:uri        (tasks-uri slug)
                                                     :params     {:name        name
                                                                  :description description}
                                                     :on-success [::task-events/fetch-tasks slug]
                                                     :on-failure [::admin-events/create-task-failed]})))})]
    (fn []
      [antd/space (cond-> {:direction "vertical"
                           :align     alignment}
                    (= alignment "center") (assoc :style {:width "100%"}))
       [antd/input (get-input-attributes :name {:spec :tasks/name})]
       [antd/input (get-input-attributes :description {:spec :tasks/description})]
       [antd/button (get-submit-attributes) "Save"]])))

(defn update-task-form
  [{:keys [id] :as task}]
  (let [{::form/keys [get-input-attributes get-submit-attributes submitting?-state]}
        (form/form {:form-key        [::update-task id]
                    :initial-values  task
                    :request-builder (fn [task]
                                       (http/put {:uri        (str (tasks-uri @(rf/subscribe [::admin-subs/org-slug]))
                                                                   id)
                                                  :params     {:updates task}
                                                  :on-success [::admin-events/update-task-success id]
                                                  :on-failure [::admin-events/update-task-failure id]}))})]
    (fn [_task]
      [antd/space {:direction "vertical"}
       [antd/input (get-input-attributes :name {:spec :tasks/name})]
       [antd/input (get-input-attributes :description {:spec :tasks/description})]
       [antd/space
        [antd/button (get-submit-attributes) "Save"]
        [antd/button {:disabled @submitting?-state
                      :on-click #(rf/dispatch [::admin-events/hide-update-task-form id])}
         "Cancel"]]])))

(defn task-list-element [{:keys [id name description] :as task}]
  (if @(rf/subscribe [::admin-subs/show-update-task-form? id])
    [update-task-form task]
    [antd/space {:direction "vertical"}
     [antd/text {:strong true} name]
     [antd/text description]]))

(defn archive-button [{:keys [id]}]
  (when (and @(rf/subscribe [::admin-subs/user-is-admin?])
             (not @(rf/subscribe [::admin-subs/show-update-task-form? id])))
    [antd/button {:on-click #(rf/dispatch [::admin-events/archive-task id])
                  :type     "link"
                  :danger   true}
     "Archive"]))

(defn update-button [{:keys [id]}]
  (when (and @(rf/subscribe [::admin-subs/user-is-admin?])
             (not @(rf/subscribe [::admin-subs/show-update-task-form? id])))
    [antd/button {:on-click #(rf/dispatch [::admin-events/show-update-task-form id])
                  :type     "link"}
     "Update"]))

(defn tasks-list []
  (r/with-let [admin? (rf/subscribe [::admin-subs/user-is-admin?])]
    (if-let [tasks (seq @(rf/subscribe [::admin-subs/tasks]))]
      [antd/list {:dataSource tasks
                  :renderItem (fn [task]
                                (antd/list-item {:actions [[archive-button task]
                                                           [update-button task]]}
                                                [task-list-element task]))
                  :header     [antd/title {:level 4
                                           :style {:margin-bottom "0px"}}
                               "Tasks"]
                  :footer     (when @admin?
                                [create-task-form "left"])}]
      [:<>
       [antd/title {:level 4
                    :style {:margin-bottom "0px"
                            :padding       "12px 0px"}}
        "Tasks"]
       [antd/divider {:style {:margin-top "0px"}}]
       [antd/row {:justify "center"}
        [antd/col
         (if @admin?
           [:<>
            [:p "You have no tasks! Create a task to get started."]
            [create-task-form "center"]]
           [:p "You have no tasks! Ask your administrator to create some."])]]])))

(defn members-list []
  [antd/list {:dataSource (concat (for [member @(rf/subscribe [::admin-subs/joined-members])]
                                    (if (= (:id member)
                                           (:id @(rf/subscribe [::subs/user-info])))
                                      (str (:name member) " (you)")
                                      (:name member)))
                                  (for [member @(rf/subscribe [::admin-subs/invited-members])]
                                    (str (:email member) " (invited)")))
              :renderItem (fn [text]
                            (antd/list-item text))
              :header     [antd/space {:align "center"}
                           [antd/title {:level 4
                                        :style {:margin-bottom "0px"}}
                            "Members"]
                           (when @(rf/subscribe [::admin-subs/user-is-admin?])
                             [invite-member-form])]
              :split      false}])

(defn organization-page [{:keys [slug]}]
  (let [{:keys [name]} @(rf/subscribe [::subs/organization slug])]
    (if-not name
      [components/full-page-spinner]
      [page-container/org-scoped-page-container
       [antd/page-header
        (when @(rf/subscribe [::admin-subs/user-is-admin?])
          [members-list])
        [tasks-list]]])))
