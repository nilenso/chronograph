(ns chronograph-web.events.organization-invites
  (:require [re-frame.core :as rf]
            [chronograph-web.db.organization-invites :as org-invites-db]
            [chronograph-web.events.organization :as org-events]
            [chronograph-web.api-client :as api]
            [chronograph-web.config :as config]
            [chronograph-web.utils.fetch-events :as fetch-events]))

(rf/reg-event-fx
  ::reject-invite
  (fetch-events/http-request-creator
   (fn [_ [_ invited-org-slug]]
     {:http-xhrio (api/reject-invite invited-org-slug
                                     [::reject-invite-succeeded invited-org-slug]
                                     [::reject-invite-failed])})))

(rf/reg-event-fx
  ::reject-invite-succeeded
  (fetch-events/http-success-handler
   (fn [{:keys [db]} [_ slug]]
     {:db (org-invites-db/remove-invite-by-slug db slug)})))

(rf/reg-event-fx
  ::reject-invite-failed
  (fn [_ _]
    {:flash-error {:content (str "Failed to reject the invite "
                                 (:frown config/emojis)
                                 " Please try again.")}}))

(rf/reg-event-fx
  ::accept-invite
  (fetch-events/http-request-creator
   (fn [_ [_ invited-org-slug]]
     {:http-xhrio (api/accept-invite invited-org-slug
                                     [::accept-invite-succeeded invited-org-slug]
                                     [::accept-invite-failed])})))

(rf/reg-event-fx
  ::accept-invite-succeeded
  (fetch-events/http-success-handler
   (fn [{:keys [db]} [_ slug]]
     {:db (org-invites-db/remove-invite-by-slug db slug)
      :fx [[:dispatch [::org-events/fetch-organizations]]]})))

(rf/reg-event-fx
  ::accept-invite-failed
  (fn [_ _]
    {:flash-error {:content (str "Failed to accept the invite "
                                 (:frown config/emojis)
                                 " Please try again.")}}))

(rf/reg-event-fx
  ::fetch-invited-orgs
  (fn [_ _]
    {:http-xhrio (api/fetch-invited-orgs [::fetch-invited-orgs-success]
                                         [::fetch-invited-orgs-failed])}))

(rf/reg-event-db
  ::fetch-invited-orgs-success
  (fn [db [_ invited-orgs]]
    (org-invites-db/add-invited-orgs db invited-orgs)))

(rf/reg-event-fx
  ::fetch-invited-orgs-failed
  (fn [_ _]
    {:flash-error {:content (str "Oh no, something went wrong! "
                                 (:frown config/emojis)
                                 " Please refresh the page!")}}))

