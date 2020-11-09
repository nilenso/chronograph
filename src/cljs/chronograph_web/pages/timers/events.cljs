(ns chronograph-web.pages.timers.events
  (:require [re-frame.core :as rf]
            [chronograph-web.events.routing :as routing-events]
            [chronograph-web.events.organization :as org-events]
            [chronograph-web.events.timer :as timer-events]
            [chronograph-web.db.organization-invites :as org-invites-db]
            [chronograph-web.utils.time :as time]
            [chronograph-web.http :as http]
            [chronograph-web.config :as config]
            [chronograph-web.db :as db]
            [chronograph-web.api-client :as api]))

(defmethod routing-events/on-route-change-event
  :timers-list
  [_]
  ::timers-page-navigated)

(rf/reg-event-fx
  ::timers-page-navigated
  (fn [_ _]
    {:fx [[:dispatch [::fetch-invited-orgs]]
          [:dispatch [::timer-events/fetch-timers (time/current-calendar-date)]]]}))

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
  ::show-create-timer-widget
  (fn [db _]
    (db/set-in-page-state db [:show-create-timer-widget] true)))

(rf/reg-event-db
  ::dismiss-create-timer-widget
  (fn [db _]
    (db/set-in-page-state db [:show-create-timer-widget] false)))

(rf/reg-event-fx
  ::create-timer-succeeded
  (fn [_ _]
    {:fx [[:dispatch [::dismiss-create-timer-widget]]
          [:dispatch [::timer-events/fetch-timers (time/current-calendar-date)]]]}))

(defn- flash-error-effect
  [message]
  {:flash-error {:content message}})

(rf/reg-event-fx
  ::flash-error
  (fn [_ [_ message]]
    (flash-error-effect message)))

(rf/reg-event-fx
  ::create-timer-failed
  (fn [_ _]
    (flash-error-effect (str "We couldn't start your timer "
                             (:frown config/emojis)
                             " Please try again!"))))

(rf/reg-event-fx
  ::start-timer
  (fn [_ [_ timer-id]]
    {:fx [[:http-xhrio (api/start-timer timer-id
                                        [::timer-events/fetch-timers (time/current-calendar-date)]
                                        [::flash-error (str "We couldn't start your timer "
                                                            (:frown config/emojis)
                                                            " Please try again!")])]]}))

(rf/reg-event-fx
  ::stop-timer
  (fn [_ [_ timer-id]]
    {:fx [[:http-xhrio (api/stop-timer timer-id
                                       [::timer-events/fetch-timers (time/current-calendar-date)]
                                       [::flash-error (str "We couldn't stop your timer "
                                                           (:frown config/emojis)
                                                           " Please try again!")])]]}))
