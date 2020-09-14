(ns chronograph-web.events.routing
  (:require [re-frame.core :as rf]
            [chronograph-web.pages.organization.events :as org-events]))


(defn- on-navigate
  [{:keys [handler]
    :as route}]
  (case handler
    :organization-show {:http-xhrio (org-events/get-organization route)}
    nil))


(rf/reg-event-fx
 ::set-page
 (fn [db [_ route]]
   (merge
    (on-navigate route)
    {:db (assoc db :page route)})))


(rf/reg-event-fx
  ::set-token
  (fn [_ [_ token]]
    {:history-token token}))
