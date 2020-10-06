(ns chronograph-web.pages.pending-invites.events
  (:require [re-frame.core :as rf]
            [chronograph-web.pages.pending-invites.db :as pending-invites-db]
            [chronograph-web.http :as http]
            [chronograph-web.db :as db]))

(rf/reg-event-fx
 ::reject-invite
 (fn [{:keys [db]} [_ id]]
    {:http-xhrio (http/delete (str "/api/organizations/invited/" (:slug (pending-invites-db/invite-by-id db id)))
                              {:on-success [::reject-invite-succeeded id]
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
       (pending-invites-db/remove-invite id))))

(rf/reg-event-fx
 ::accept-invite
 (fn [{:keys [db]} [_ id]]
   {:db            (pending-invites-db/remove-invite db id)
    :history-token (str "/organization/" (:slug (pending-invites-db/invite-by-id db id)))}))

(rf/reg-event-fx
 ::page-mounted
 (fn [_ _]
   {:http-xhrio (http/get "/api/organizations/invited"
                          {:on-success [::fetch-invited-orgs-success]
                           :on-failure [::fetch-invited-orgs-failed]})}))

(rf/reg-event-db
 ::fetch-invited-orgs-success
 (fn [db [_ invited-orgs]]
   (pending-invites-db/add-invited-orgs db invited-orgs)))

(rf/reg-event-db
 ::fetch-invited-orgs-failed
 (fn [db _]
   (db/report-error db ::error-fetch-invited-orgs-failed)))
