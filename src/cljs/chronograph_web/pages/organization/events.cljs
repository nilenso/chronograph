(ns chronograph-web.pages.organization.events
  (:require [re-frame.core :as rf]
            [chronograph-web.api-client :as api]
            [chronograph-web.http :as http]
            [chronograph-web.pages.organization.db :as page-db]
            [chronograph-web.db.organization :as org-db]
            [chronograph-web.events.tasks :as task-events]
            [chronograph-web.config :as config]
            [chronograph-web.events.routing :as routing-events]
            [chronograph-web.db.organization-context :as org-ctx-db]))

(def ^:private get-organization-uri
  "/api/organizations/")

(defmethod routing-events/on-route-change-event :organization-show [_]
  [::organization-page-navigated])

(rf/reg-event-fx
  ::organization-page-navigated
  (fn [{:keys [db]} _]
    {:fx [[:dispatch [::fetch-organization (org-ctx-db/current-organization-slug db)]]
          [:dispatch [::task-events/fetch-tasks (org-ctx-db/current-organization-slug db)]]]}))

(rf/reg-event-fx
  ::fetch-organization
  (fn [_ [_ slug]]
   ;; TODO: optimize fetch and rendering in case we already have
   ;; data for the organization, in our db.
    {:http-xhrio (http/get {:uri        (str get-organization-uri slug)
                            :on-success [::fetch-organization-success]
                            :on-failure [::fetch-organization-fail slug]})}))

(rf/reg-event-fx
  ::fetch-organization-success
  (fn [{:keys [db]} [_ organization]]
    (let [new-db (org-db/add-org db organization)]
      {:db new-db
       :fx [(when (page-db/user-is-admin? new-db)
              [:dispatch [::fetch-members (org-ctx-db/current-organization-slug new-db)]])]})))

(rf/reg-event-fx
  ::fetch-organization-fail
  (fn [_ _]
    {:flash-error {:content (str "Oh no, something went wrong! "
                                 (:frown config/emojis)
                                 " Please refresh the page!")}}))

(rf/reg-event-fx
  ::fetch-members
  (fn [_ [_ slug]]
    {:http-xhrio (http/get {:uri        (str get-organization-uri slug "/members")
                            :on-success [::fetch-members-succeeded]
                            :on-failure [::fetch-members-failed slug]})}))

(rf/reg-event-db
  ::fetch-members-succeeded
  (fn [db [_ {:keys [invited joined]}]]
    (-> db
        (page-db/add-invited-members invited)
        (page-db/add-joined-members joined))))

(rf/reg-event-fx
  ::fetch-members-failed
  (fn [_ _]
    {:flash-error {:content (str "Oh no, something went wrong! "
                                 (:frown config/emojis)
                                 " Please refresh the page!")}}))

(rf/reg-event-db
  ::invite-member-succeeded
  (fn [db [_ member]]
    (page-db/add-invited-member db member)))

(rf/reg-event-fx
  ::invite-member-failed
  (fn [_ [_ {:keys [status]}]]
    (if (= status 409)
      {:flash-error {:content "That user is already in the organization!"}}
      {:flash-error {:content (str "Failed to send the invitation "
                                   (:frown config/emojis)
                                   " Please try again.")}})))

(rf/reg-event-fx
  ::create-task-failed
  (fn [_ _]
    {:flash-error {:content (str "Failed to create the task "
                                 (:frown config/emojis)
                                 " Please try again.")}}))

(rf/reg-event-fx
  ::archive-task
  (fn [{:keys [db]} [_ task-id]]
    (let [slug (org-ctx-db/current-organization-slug db)]
      {:http-xhrio (api/archive-task
                    slug
                    task-id
                    [::archive-task-success slug task-id]
                    [::archive-task-failure])})))

(rf/reg-event-fx
  ::archive-task-success
  (fn [{db :db} [_ slug id]]
    {:db (update-in db
                    [:tasks]
                    dissoc
                    id)
     :fx [[:dispatch [::task-events/fetch-tasks slug]]]}))

(rf/reg-event-db
  ::archive-task-failure
  (fn [db _] db))

(rf/reg-event-db
  ::show-update-task-form
  (fn [db [_ task-id]]
    (page-db/set-show-update-task-form db task-id true)))

(rf/reg-event-db
  ::hide-update-task-form
  (fn [db [_ task-id]]
    (page-db/set-show-update-task-form db task-id false)))

(rf/reg-event-fx
  ::update-task-success
  (fn [{:keys [db]} [_ task-id]]
    {:db (page-db/set-show-update-task-form db task-id false)
     :fx [[:dispatch [::task-events/fetch-tasks (org-ctx-db/current-organization-slug db)]]]}))

(rf/reg-event-fx
  ::update-task-failure
  (fn [_ _]
    {:flash-error {:content (str "Failed to update the task "
                                 (:frown config/emojis)
                                 " Please try again.")}}))
