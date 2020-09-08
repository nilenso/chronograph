(ns chronograph-web.pages.create-organization.events
  (:require [re-frame.core :as rf]
            [chronograph-web.db :as db]
            [chronograph-web.http :as http]
            [ajax.core :as ajax]
            [day8.re-frame.http-fx]))

(def ^:private create-organization-uri "/api/organizations")

(def ^:private root-path [:create-organization])
(def ^:private status-path [root-path :status])
(defn- form-params-path [k] [root-path :form-params k])

(rf/reg-event-db
  ::create-organization-form-update
  (fn [db [_ k v]]
    (-> db
        (assoc-in status-path :editing)
        (assoc-in (form-params-path k) v))))

(rf/reg-event-fx
  ::create-organization-form-submit
  (fn [{:keys [db]} _]
    {:db (assoc-in db status-path :creating)
     :http-xhrio (http/post create-organization-uri
                            {:params {:name (get-in db (form-params-path :name))
                                      :slug (get-in db (form-params-path :slug))}
                             :on-success [::create-organization-succeeded]
                             :on-failure [::create-organization-failed]})}))

(rf/reg-event-db
  ::create-organization-succeeded
  (fn [db [_ {:keys [id] :as response}]]
    (-> db
        (assoc-in status-path :created)
        (assoc-in [:organizations id] response))))

(rf/reg-event-db
  ::create-organization-failed
  (fn [db [_ {:keys [id] :as response}]]
    (assoc-in db status-path :failed)))
