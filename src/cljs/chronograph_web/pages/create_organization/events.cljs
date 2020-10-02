(ns chronograph-web.pages.create-organization.events
  (:require [re-frame.core :as rf]
            [chronograph-web.http :as http]
            [day8.re-frame.http-fx]))

(def ^:private create-organization-uri "/api/organizations/")

(def ^:private page-state-key :page-state)
(def ^:private root-path :create-organization)
(def ^:private status-path [page-state-key root-path :status])
(defn- form-params-path [k] [page-state-key root-path :form-params k])

(rf/reg-event-db
  ::create-organization-form-update
  (fn [db [_ k v]]
    (-> db
        (assoc-in status-path :editing)
        (assoc-in (form-params-path k) v))))

(rf/reg-event-fx
  ::create-organization-form-submit
  (fn [{:keys [db]} _]
    {:db         (assoc-in db status-path :creating)
     :http-xhrio (http/post {:uri create-organization-uri
                             :params     {:name (get-in db (form-params-path :name))
                                          :slug (get-in db (form-params-path :slug))}
                             :on-success [::create-organization-succeeded]
                             :on-failure [::create-organization-failed]})}))

(defn- organization-url [slug]
  (str "/organizations/" slug))

(rf/reg-event-fx
  ::create-organization-succeeded
  (fn [{:keys [db]} [_ {:keys [slug] :as response}]]
    {:history-token (organization-url slug)
     :db            (-> db
                        (assoc-in status-path :created)
                        (assoc-in [:organizations slug] response))}))

(rf/reg-event-db
  ::create-organization-failed
  (fn [db _]
    (assoc-in db status-path :failed)))
