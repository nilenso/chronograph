(ns chronograph-web.pages.create-organization.events
  (:require [re-frame.core :as rf]
            [chronograph-web.routes :as routes]
            [chronograph-web.db.organization :as org-db]
            [chronograph-web.config :as config]))

(rf/reg-event-fx
  ::create-organization-succeeded
  (fn [{:keys [db]} [_ {:keys [slug] :as response}]]
    {:history-token (routes/path-for :organization-show :slug slug)
     :db            (org-db/add-org db response)}))

(rf/reg-event-fx
  ::create-organization-failed
  (fn [_ _]
    {:flash-error {:content (str "Failed to create the organization "
                                 (:frown config/emojis)
                                 " Please try again.")}}))
