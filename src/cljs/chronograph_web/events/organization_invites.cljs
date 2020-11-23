(ns chronograph-web.events.organization-invites
  (:require [re-frame.core :as rf]
            [chronograph-web.db.organization-invites :as org-invites-db]
            [chronograph-web.events.organization :as org-events]
            [chronograph-web.api-client :as api]
            [chronograph-web.config :as config]))

(defn- add-callback-event
  [fx event response]
  (cond-> fx
    event (conj [:dispatch (conj event response)])))

(rf/reg-event-fx
  ::reject-invite
  (fn [{:keys [db]} [_ invited-org-slug on-success on-failure]]
    {:http-xhrio (api/reject-invite invited-org-slug
                                    [::reject-invite-succeeded invited-org-slug on-success]
                                    (or on-failure
                                        [::reject-invite-failed]))}))

(rf/reg-event-fx
  ::reject-invite-failed
  (fn [_ _]
    {:flash-error {:content (str "Failed to reject the invite "
                                 (:frown config/emojis)
                                 " Please try again.")}}))

(rf/reg-event-fx
  ::reject-invite-succeeded
  (fn [{:keys [db]} [_ slug on-success response]]
    {:db (org-invites-db/remove-invite-by-slug db slug)
     :fx (-> []
             (add-callback-event on-success response))}))

(rf/reg-event-fx
  ::accept-invite
  (fn [{:keys [db]} [_ invited-org-slug on-success on-failure]]
    {:http-xhrio (api/accept-invite invited-org-slug
                                    [::accept-invite-succeeded invited-org-slug on-success]
                                    (or on-failure
                                        [::accept-invite-failed]))}))

(rf/reg-event-fx
  ::accept-invite-succeeded
  (fn [{:keys [db]} [_ slug on-success response]]
    {:db (org-invites-db/remove-invite-by-slug db slug)
     :fx (-> [[:dispatch [::org-events/fetch-organizations]]]
             (add-callback-event on-success response))}))

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

