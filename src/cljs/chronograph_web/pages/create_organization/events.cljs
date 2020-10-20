(ns chronograph-web.pages.create-organization.events
  (:require [re-frame.core :as rf]
            [day8.re-frame.http-fx]
            [chronograph-web.db :as db]
            [chronograph-web.pages.create-organization.db :as create-org-db]))

(defn- organization-url [slug]
  (str "/organizations/" slug))

(rf/reg-event-fx
  ::create-organization-succeeded
  (fn [{:keys [db]} [_ {:keys [slug] :as response}]]
    {:history-token (organization-url slug)
     :db            (-> db
                        (create-org-db/add-to-organizations response)
                        (db/remove-error ::error-create-organization-failed))}))

(rf/reg-event-db
  ::create-organization-failed
  (fn [db _]
    (db/report-error db ::error-create-organization-failed)))
