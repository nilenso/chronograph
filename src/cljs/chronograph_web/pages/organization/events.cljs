(ns chronograph-web.pages.organization.events
  (:require [re-frame.core :as rf]
            [chronograph-web.http :as http]))


(def ^:private get-organization-uri
  "/api/organizations/")


(rf/reg-event-fx
 ::fetch-organization
 (fn [_ [_ slug]]
   {:http-xhrio (http/get (str get-organization-uri slug)
                          {:on-success [::fetch-organization-success]
                           :on-failure [::fetch-organization-fail]})}))


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
