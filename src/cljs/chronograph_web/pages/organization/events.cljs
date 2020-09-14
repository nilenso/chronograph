(ns chronograph-web.pages.organization.events
  (:require [re-frame.core :as rf]
            [chronograph-web.http :as http]))


(def ^:private get-organization-uri
  "/api/organizations")


(defn get-organization
  [route]
  (http/get (str get-organization-uri
                 (get-in route [:route-params :slug]))
            {:on-success [::fetch-organization-success]
             :on-failure [::fetch-organization-fail]}))


(rf/reg-event-db
 ::fetch-organization-success
 (fn [db [_ {:keys [slug] :as organization}]]
   (assoc-in db
             [:organizations slug]
             organization)))


(rf/reg-event-db
 ::fetch-organization-fail
 (fn [db _]
   db))
