(ns chronograph-web.pages.landing.events
  (:require [re-frame.core :as rf]
            [chronograph-web.db :as db]
            [chronograph-web.db.organization-invites :as org-invites-db]
            [chronograph-web.pages.landing.db :as landing-db]
            [chronograph-web.http :as http]))

(defn- organization-url [slug]
  (str "/organizations/" slug))

(rf/reg-event-fx
  ::reject-invite
  (fn [{:keys [db]} [_ id]]
    {:http-xhrio (http/post {:uri        (str "/api/invitations/" (org-invites-db/slug-by-id db id) "/reject")
                             :on-success [::reject-invite-succeeded id]
                             :on-failure [::reject-invite-failed]})}))

(rf/reg-event-db
  ::reject-invite-failed
  (fn [db _]
    (db/report-error db ::error-reject-invite-failed)))

(rf/reg-event-db
  ::reject-invite-succeeded
  (fn [db [_ id]]
    (-> db
        (db/remove-error ::error-reject-invite-failed)
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
    {:db            (-> db
                        (db/remove-error ::error-accept-invite-failed)
                        (org-invites-db/move-accepted-org-to-organizations id))}))

(rf/reg-event-db
  ::accept-invite-failed
  (fn [db _]
    (db/report-error db ::error-accept-invite-failed)))

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

(rf/reg-event-db
  ::fetch-invited-orgs-failed
  (fn [db _]
    (db/report-error db ::error-fetch-invited-orgs-failed)))

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
    {:history-token (organization-url slug)
     :db            (-> db
                        (landing-db/add-to-organizations response)
                        (db/remove-error ::error-create-organization-failed))}))

(rf/reg-event-db
  ::create-organization-failed
  (fn [db _]
    (db/report-error db ::error-create-organization-failed)))
