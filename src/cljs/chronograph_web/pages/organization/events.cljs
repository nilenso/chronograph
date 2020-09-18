(ns chronograph-web.pages.organization.events
  (:require [goog.string :as gstring]
            [re-frame.core :as rf]
            [chronograph-web.http :as http]))

(def ^:private get-organization-uri
  "/api/organizations/")

(defn- fetch-tasks-uri [organization-id]
  (gstring/format "/api/organizations/%s/tasks" organization-id))

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
    (assoc-in db
              [:organizations slug]
              organization)))

(rf/reg-event-db
  ::fetch-organization-fail
  (fn [db [_ slug]]
    (assoc-in db
              [:organizations slug]
              ::not-found)))

(rf/reg-event-fx
 ::fetch-tasks
 (fn [_ [_ organization-id]]
   {:http-xhrio (http/get (fetch-tasks-uri organization-id)
                          {:on-success [::fetch-tasks-success]
                           :on-failure [::fetch-tasks-failure]})}))
