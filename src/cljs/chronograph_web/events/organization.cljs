(ns chronograph-web.events.organization
  (:require [chronograph-web.config :as config]
            [chronograph-web.http :as http]
            [re-frame.core :as rf]))

(def ^:private base-url
  (str config/api-root "/organizations/"))

(rf/reg-event-fx
  ::fetch-organizations
  (fn [_ [_ slug]]
   ;; TODO: optimize fetch and rendering in case we already have
   ;; data for the organization, in our db.
    {:http-xhrio (http/get {:uri base-url
                            :on-success [::fetch-organizations-success]
                            :on-failure [::fetch-organizations-fail slug]})}))

(rf/reg-event-db
  ::fetch-organizations-success
  (fn [db [_ organizations]]
    (assoc-in db [:organizations]
              (zipmap (map :slug organizations) organizations))))

(rf/reg-event-db
  ::fetch-organizations-fail
  (fn [db [_ _]]
    db))
