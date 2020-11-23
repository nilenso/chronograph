(ns chronograph-web.pages.timers.events
  (:require [re-frame.core :as rf]
            [chronograph-web.events.routing :as routing-events]
            [chronograph-web.events.timer :as timer-events]
            [chronograph-web.events.tasks :as task-events]
            [chronograph-web.db.organization-context :as page-db]
            [chronograph-web.utils.time :as time]
            [chronograph-web.events.organization-invites :as org-invites-events]
            [chronograph-web.config :as config]
            [chronograph-web.db :as db]
            [chronograph-web.api-client :as api]))

(defmethod routing-events/on-route-change-event
  :timers-list
  [_]
  ::timers-page-navigated)

(rf/reg-event-fx
  ::timers-page-navigated
  (fn [{:keys [db]} _]
    {:fx [[:dispatch [::org-invites-events/fetch-invited-orgs]]
          [:dispatch [::task-events/fetch-tasks (page-db/current-organization-slug db)]]
          [:dispatch [::timer-events/fetch-timers (time/current-calendar-date)]]]}))

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

(rf/reg-event-fx
  ::delete-timer
  (fn [_ [_ timer-id]]
    {:fx [[:http-xhrio (api/delete-timer timer-id
                                         [::timer-events/fetch-timers (time/current-calendar-date)]
                                         [::flash-error (str "We couldn't delete your timer "
                                                             (:frown config/emojis)
                                                             " Please try again!")])]]}))
