(ns chronograph-web.events.organization
  (:require [re-frame.core :as rf]
            [chronograph-web.api-client :as api]
            [chronograph-web.db.organization :as org-db]))

(rf/reg-event-fx
  ::fetch-organizations
  (fn [_ _]
    {:http-xhrio (api/fetch-organizations [::fetch-organizations-success]
                                          [::fetch-organizations-fail])}))

(rf/reg-event-db
  ::fetch-organizations-success
  (fn [db [_ organizations]]
    (org-db/add-organizations db organizations)))

(rf/reg-event-db
  ::fetch-organizations-fail
  (fn [db [_ _]]
    db))
