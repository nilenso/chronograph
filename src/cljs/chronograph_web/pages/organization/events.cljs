(ns chronograph-web.pages.organization.events
  (:require [re-frame.core :as rf]
            [chronograph-web.http :as http]
            [chronograph-web.pages.organization.db :as org-db]
            [chronograph-web.db :as db]
            [goog.string :as gstring]))

(def ^:private get-organization-uri
  "/api/organizations/")

(defn- tasks-uri [slug]
  (gstring/format "/api/organizations/%s/tasks/" slug))

(def ^:private root-path :create-task)
(def ^:private status-path [root-path :status])
(defn- form-params-path [k] [root-path :form-params k])

(rf/reg-event-fx
  ::fetch-organization
  (fn [_ [_ slug]]
   ;; TODO: optimize fetch and rendering in case we already have
   ;; data for the organization, in our db.
    {:http-xhrio (http/get (str get-organization-uri slug)
                           {:on-success [::fetch-organization-success]
                            :on-failure [::fetch-organization-fail slug]})}))

(rf/reg-event-fx
 ::fetch-members
 (fn [_ [_ slug]]
   {:http-xhrio (http/get (str get-organization-uri slug "/members")
                          {:on-success [::fetch-members-succeeded]
                           :on-failure [::fetch-members-failed slug]})}))

(rf/reg-event-db
  ::fetch-members-succeeded
  (fn [db [_ {:keys [invited joined]}]]
    (-> db
        (org-db/add-invited-members invited)
        (org-db/add-joined-members joined))))

(rf/reg-event-db
  ::fetch-organization-success
  (fn [db [_ {:keys [slug] :as organization}]]
    (-> db
        (assoc-in [:organizations slug] organization)
        (assoc-in [:current-organization] organization))))

(rf/reg-event-db
  ::fetch-organization-fail
  (fn [db _]
    (db/report-error db ::error-org-not-found)))

(rf/reg-event-db
  ::email-input-changed
  (fn [db [_ email-value]]
    (org-db/set-in-add-member-form db :email email-value)))

(rf/reg-event-fx
  ::invite-button-clicked
  (fn [{:keys [db]} _]
    {:http-xhrio (http/post (str "/api/organizations/"
                                 (org-db/slug db)
                                 "/members")
                            {:params     {:email (org-db/get-from-add-member-form db :email)}
                             :on-success [::invite-member-succeeded]
                             :on-failure [::invite-member-failed]})
     :db         (-> db
                     (org-db/set-in-add-member-form :email "")
                     (db/remove-error ::error-invite-member-failed))}))

(rf/reg-event-db
  ::invite-member-succeeded
  (fn [db [_ member]]
    (org-db/add-invited-member db member)))

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
    {:http-xhrio (http/get (tasks-uri slug)
                           {:on-success [::fetch-tasks-success]
                            :on-failure [::fetch-tasks-failure]})}))

(rf/reg-event-db
  ::fetch-tasks-success
  (fn [db [_ tasks]]
    (assoc-in db [:tasks]
              (zipmap (map :id tasks) tasks))))

(rf/reg-event-db
  ::fetch-tasks-failure
  (fn [db [_ _]]
    db))

(rf/reg-event-db
  ::create-task-form-update
  (fn [db [_ k v]]
    (-> db
        (assoc-in status-path :editing)
        (assoc-in (form-params-path k) v))))

(rf/reg-event-fx
  ::create-task-form-submit
  (fn [{:keys [db]} _]
    {:db         (assoc-in db status-path :creating)
     :http-xhrio (http/post (tasks-uri (-> db :current-organization :slug))
                            {:params     {:name (get-in db (form-params-path :name))
                                          :description (get-in db (form-params-path :description))}
                             :on-success [::create-task-success]
                             :on-failure [::create-task-failure]})}))

(rf/reg-event-fx
  ::create-task-success
  (fn [{:keys [db]} [_ _]]
    (let [slug (get-in db [:current-organization :slug])]
      {:db (dissoc db root-path)
       :fx [[:dispatch [::fetch-tasks slug]]]})))

(rf/reg-event-db
  ::create-task-failure
  (fn [db _]
    (assoc-in db status-path :failed)))

(rf/reg-event-fx
  ::archive-task
  (fn [{:keys [db]} [_ task-id]]
    (let [slug (-> db :current-organization :slug)
          archive-url (str  (tasks-uri slug) task-id "/archive")]
      {:http-xhrio (http/put archive-url
                             {:on-success [::fetch-tasks slug]
                              :on-failure [::archive-task-failure]})})))

(rf/reg-event-db
  ::archive-task-failure
  (fn [db _] db))

(rf/reg-event-db
  ::show-update-form
  (fn [db [_ {:keys [id name description] :as _task}]]
    (-> db
        (assoc-in [:tasks id :is-updating] true)
        (assoc-in [:update-task id :form-params]
                  {:name name
                   :description description}))))

(rf/reg-event-db
  ::update-task-form-update
  (fn [db [_ task-id k v]]
    (-> db
        (assoc-in [:update-task task-id :status] :editing)
        (assoc-in [:update-task task-id :form-params k] v))))

(rf/reg-event-db
  ::cancel-update-task-form
  (fn [db [_ task-id]]
    (let [_update-task-forms (dissoc (-> :update-task :db) task-id)])
    (-> db
        (assoc-in [:tasks task-id :is-updating] false)
        (update-in [:update-task] dissoc task-id))))

(rf/reg-event-fx
  ::update-task-form-submit
  (fn [{:keys [db]} [_ task-id]]
    (let [slug (-> db :current-organization :slug)
          update-url (str (tasks-uri slug) task-id)]
      {:db         (assoc-in db [:update-task task-id :status] :saving)
       :http-xhrio (http/put update-url
                             {:params     {:updates (get-in db [:update-task task-id :form-params])}
                              :on-success [::update-task-success task-id]
                              :on-failure [::update-task-failure task-id]})})))

(rf/reg-event-fx
  ::update-task-success
  (fn [{:keys [db]} [_ task-id]]
    {:db (-> db
             (assoc-in [:tasks task-id :is-updating] false)
             (update-in [:update-task] dissoc task-id))
     :fx [[:dispatch [::fetch-tasks (-> db :current-organization :slug)]]]}))

(rf/reg-event-db
  ::update-task-failure
  (fn [db _] db))
