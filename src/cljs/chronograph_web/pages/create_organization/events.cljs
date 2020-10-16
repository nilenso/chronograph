(ns chronograph-web.pages.create-organization.events
  (:require [re-frame.core :as rf]
            [day8.re-frame.http-fx]))

(defn- organization-url [slug]
  (str "/organizations/" slug))

(rf/reg-event-fx
  ::create-organization-succeeded
  (fn [{:keys [db]} [_ {:keys [slug] :as response}]]
    {:history-token (organization-url slug)
     :db            (assoc-in db
                              [:organizations slug]
                              response)}))

(rf/reg-event-db
  ::create-organization-failed
  (fn [db _] db))
