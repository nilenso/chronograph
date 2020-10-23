(ns chronograph-web.pages.organization.events
  (:require [re-frame.core :as rf]
            [chronograph-web.http :as http]
            [chronograph-web.pages.organization.db :as page-db]
            [chronograph.utils.data :as datautils]
            [chronograph-web.db.organization :as org-db]
            [chronograph-web.config :as config]))

(def ^:private get-organization-uri
  "/api/organizations/")

(defn- tasks-uri [slug]
  (str "/api/organizations/"
       slug
       "/tasks/"))

(rf/reg-event-fx
  ::fetch-organization
  (fn [_ [_ slug]]
   ;; TODO: optimize fetch and rendering in case we already have
   ;; data for the organization, in our db.
    {:http-xhrio (http/get {:uri        (str get-organization-uri slug)
                            :on-success [::fetch-organization-success]
                            :on-failure [::fetch-organization-fail slug]})}))

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

(rf/reg-event-db
  ::fetch-organization-success
  (fn [db [_ organization]]
    (org-db/add-org db organization)))

(rf/reg-event-fx
  ::fetch-organization-fail
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
  ::fetch-members-failed
  (fn [_ _]
    {:flash-error {:content (str "Oh no, something went wrong! "
                                 (:frown config/emojis)
                                 " Please refresh the page!")}}))

(rf/reg-event-fx
  ::fetch-tasks
  (fn [_ [_ slug]]
    {:http-xhrio (http/get {:uri        (tasks-uri slug)
                            :on-success [::fetch-tasks-success]
                            :on-failure [::fetch-tasks-failure]})}))

(rf/reg-event-db
  ::fetch-tasks-success
  (fn [db [_ tasks]]
    (update db :tasks merge (datautils/normalize-by :id tasks))))

(rf/reg-event-fx
  ::fetch-tasks-failure
  (fn [_ _]
    {:flash-error {:content (str "Oh no, something went wrong! "
                                 (:frown config/emojis)
                                 " Please refresh the page!")}}))

(rf/reg-event-fx
  ::create-task-failed
  (fn [_ _]
    {:flash-error {:content (str "Failed to create the task "
                                 (:frown config/emojis)
                                 " Please try again.")}}))

(rf/reg-event-fx
  ::archive-task
  (fn [{:keys [db]} [_ task-id]]
    (let [slug        (page-db/slug db)
          archive-url (str (tasks-uri slug) task-id "/archive")]
      {:http-xhrio (http/put {:uri        archive-url
                              :on-success [::archive-task-success slug task-id]
                              :on-failure [::archive-task-failure]})})))

(rf/reg-event-fx
  ::archive-task-success
  (fn [{db :db} [_ slug id]]
    {:db (update-in db
                    [:tasks]
                    dissoc
                    id)
     :fx [[:dispatch [::fetch-tasks slug]]]}))

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
    {:db (assoc-in db [:tasks task-id :is-updating] false)
     :fx [[:dispatch [::fetch-tasks (page-db/slug db)]]]}))

(rf/reg-event-fx
  ::update-task-failure
  (fn [_ _]
    {:flash-error {:content (str "Failed to update the task "
                                 (:frown config/emojis)
                                 " Please try again.")}}))
