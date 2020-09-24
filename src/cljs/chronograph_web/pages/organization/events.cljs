(ns chronograph-web.pages.organization.events
  (:require [goog.string :as gstring]
            [re-frame.core :as rf]
            [chronograph-web.http :as http]))

(def ^:private get-organization-uri
  "/api/organizations/")

(defn- tasks-uri [organization-id]
  (gstring/format "/api/organizations/%s/tasks" organization-id))

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

(rf/reg-event-db
  ::fetch-organization-success
  (fn [db [_ {:keys [slug] :as organization}]]
    (-> db
        (assoc-in [:organizations slug] organization)
        (assoc-in [:current-organization] organization))))

(rf/reg-event-db
  ::fetch-organization-fail
  (fn [db [_ slug]]
    (assoc-in db
              [:organizations slug]
              ::not-found)))

(rf/reg-event-fx
 ::fetch-tasks
 (fn [_ [_ organization-id]]
   {:http-xhrio (http/get (tasks-uri organization-id)
                          {:on-success [::fetch-tasks-success]
                           :on-failure [::fetch-tasks-failure]})}))

(rf/reg-event-db
  ::fetch-tasks-success
  (fn [db [_ tasks]]
    (assoc-in db
              [:tasks]
              tasks)))

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
     :http-xhrio (http/post (tasks-uri (-> db :current-organization :id) )
                            {:params     {:name (get-in db (form-params-path :name))
                                          :slug (get-in db (form-params-path :slug))}
                             :on-success [::create-task-success]
                             :on-failure [::create-task-failure]})}))

(rf/reg-event-fx
  ::create-task-success
  (fn [{:keys [db]} [_ _]]
    {:db db}))

(rf/reg-event-db
  ::create-task-failure
  (fn [db _]
    (assoc-in db status-path :failed)))
