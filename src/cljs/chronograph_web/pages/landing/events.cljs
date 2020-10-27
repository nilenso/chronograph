(ns chronograph-web.pages.landing.events
  (:require [re-frame.core :as rf]
            [chronograph-web.events.organization :as org-events]
            [chronograph-web.db :as db]
            [chronograph-web.db.organization-invites :as org-invites-db]
            [chronograph-web.pages.landing.db :as landing-db]
            [chronograph-web.http :as http]
            [chronograph-web.routes :as routes]
            [chronograph-web.config :as config]))

(rf/reg-event-fx
  ::reject-invite
  (fn [{:keys [db]} [_ id]]
    {:http-xhrio (http/post {:uri        (str "/api/invitations/" (org-invites-db/slug-by-id db id) "/reject")
                             :on-success [::reject-invite-succeeded id]
                             :on-failure [::reject-invite-failed]})}))

(rf/reg-event-fx
  ::reject-invite-failed
  (fn [_ _]
    {:flash-error {:content (str "Failed to reject the invite "
                                 (:frown config/emojis)
                                 " Please try again.")}}))

(rf/reg-event-db
  ::reject-invite-succeeded
  (fn [db [_ id]]
    (-> db
        (org-invites-db/remove-invite id))))

(rf/reg-event-fx
  ::accept-invite
  (fn [{:keys [db]} [_ id]]
    {:http-xhrio (http/post {:uri        (str "/api/invitations/" (org-invites-db/slug-by-id db id) "/accept")
                             :on-success [::accept-invite-succeeded id]
                             :on-failure [::accept-invite-failed]})}))

(rf/reg-event-fx
  ::accept-invite-succeeded
  (fn [{:keys [db]} [_ id]]
    {:db (org-invites-db/remove-invite db id)
     :fx [[:dispatch [::org-events/fetch-organizations]]]}))

(rf/reg-event-fx
  ::accept-invite-failed
  (fn [_ _]
    {:flash-error {:content (str "Failed to accept the invite "
                                 (:frown config/emojis)
                                 " Please try again.")}}))

(rf/reg-event-fx
  ::fetch-invited-orgs
  (fn [_ _]
    {:http-xhrio (http/get {:uri        "/api/invitations"
                            :on-success [::fetch-invited-orgs-success]
                            :on-failure [::fetch-invited-orgs-failed]})}))

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

(rf/reg-event-db
  ::show-create-org-form
  (fn [db _]
    (db/set-in-page-state db [:show-create-org-form] true)))

(rf/reg-event-db
  ::hide-create-org-form
  (fn [db _]
    (db/set-in-page-state db [:show-create-org-form] false)))

(rf/reg-event-fx
  ::create-organization-succeeded
  (fn [{:keys [db]} [_ {:keys [slug] :as response}]]
    {:history-token (routes/path-for :organization-show :slug slug)
     :db            (landing-db/add-to-organizations db response)}))

(rf/reg-event-fx
  ::create-organization-failed
  (fn [_ _]
    {:flash-error {:content (str "Failed to create the organization "
                                 (:frown config/emojis)
                                 " Please try again.")}}))
