(ns chronograph-web.pages.organization.events
  (:require [re-frame.core :as rf]
            [chronograph-web.http :as http]
            [chronograph-web.pages.organization.db :as page-db]
            [chronograph-web.db :as db]
            [chronograph.utils.data :as datautils]
            [chronograph-web.db.organization :as org-db]))

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
        (page-db/add-joined-members joined)
        (db/remove-error ::error-fetch-members-failed))))

(rf/reg-event-db
  ::fetch-organization-success
  (fn [db [_ organization]]
    (-> db
        (db/remove-error ::error-org-not-found)
        (org-db/add-org organization))))

(rf/reg-event-db
  ::fetch-organization-fail
  (fn [db _]
    (db/report-error db ::error-org-not-found)))

(rf/reg-event-db
  ::invite-member-succeeded
  (fn [db [_ member]]
    (-> db
        (page-db/add-invited-member member)
        (db/remove-error ::error-invite-member-failed))))

(rf/reg-event-db
  ::invite-member-failed
  (fn [db _]
    (db/report-error db ::error-invite-member-failed)))

(rf/reg-event-db
  ::fetch-members-failed
  (fn [db _]
    (db/report-error db ::error-fetch-members-failed)))

(rf/reg-event-fx
  ::fetch-tasks
  (fn [_ [_ slug]]
    {:http-xhrio (http/get {:uri        (tasks-uri slug)
                            :on-success [::fetch-tasks-success]
                            :on-failure [::fetch-tasks-failure]})}))

(rf/reg-event-db
  ::fetch-tasks-success
  (fn [db [_ tasks]]
    (-> db
        (update :tasks merge (datautils/normalize-by :id tasks))
        (db/remove-error ::error-fetch-tasks-failed))))

(rf/reg-event-db
  ::fetch-tasks-failure
  (fn [db [_ _]]
    (db/report-error db ::error-fetch-tasks-failed)))

(rf/reg-event-db
  ::create-task-failed
  (fn [db _]
    (db/report-error db ::error-creating-task-failed)))

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
    (assoc-in db [:tasks task-id :is-updating] true)))

(rf/reg-event-db
  ::hide-update-task-form
  (fn [db [_ task-id]]
    (-> db
        (assoc-in [:tasks task-id :is-updating] false))))

(rf/reg-event-fx
  ::update-task-success
  (fn [{:keys [db]} [_ task-id]]
    {:db (-> db
             (assoc-in [:tasks task-id :is-updating] false)
             (update-in [:update-task] dissoc task-id)
             (db/remove-error ::error-update-task-failed))
     :fx [[:dispatch [::fetch-tasks (page-db/slug db)]]]}))

(rf/reg-event-db
  ::update-task-failure
  (fn [db _]
    (db/report-error db ::error-update-task-failed)))
