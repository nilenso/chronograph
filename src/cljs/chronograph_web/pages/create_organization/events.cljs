(ns chronograph-web.pages.create-organization.events
  (:require [re-frame.core :as rf]
            [chronograph-web.db :as db]
            [chronograph-web.db.organization :as org-db]
            [chronograph-web.events.routing :as routing-events]
            [chronograph-web.routes :as routes]
            [chronograph-web.config :as config]))

(defmethod routing-events/on-route-change-event
  :new-organization
  []
  [::new-organization-page-navigated])

(rf/reg-event-db
  ::new-organization-page-navigated
  (fn [db _]
    (db/set-page-key db :new-organization)))

(rf/reg-event-fx
  ::create-organization-succeeded
  (fn [{:keys [db]} [_ {:keys [slug] :as response}]]
    {:history-token (routes/path-for :admin-page :slug slug)
     :db            (org-db/add-org db response)}))

(rf/reg-event-fx
  ::create-organization-failed
  (fn [_ _]
    {:flash-error {:content (str "Failed to create the organization "
                                 (:frown config/emojis)
                                 " Please try again.")}}))
