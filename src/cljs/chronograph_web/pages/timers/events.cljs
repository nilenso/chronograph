(ns chronograph-web.pages.timers.events
  (:require [re-frame.core :as rf]
            [chronograph-web.routes :as routes]
            [chronograph-web.events.routing :as routing-events]
            [chronograph-web.events.timer :as timer-events]
            [chronograph-web.events.tasks :as task-events]
            [chronograph-web.db.organization-context :as org-ctx-db]
            [chronograph-web.utils.time :as time]
            [chronograph-web.events.organization-invites :as org-invites-events]
            [chronograph-web.config :as config]
            [chronograph-web.db :as db]
            [chronograph-web.api-client :as api]
            [chronograph-web.components.form :as form]
            [medley.core :as medley]
            [chronograph-web.db.timers :as db-timers]
            [chronograph.utils.string :as ustring]))

(defn- date-from-route-params [{:keys [year month day] :as m}]
  (let [date (->> (select-keys m [:year :month :day])
                  (medley/map-vals #(js/parseInt %)))]
    (update date :month dec)))

(defmethod routing-events/on-route-change-event
  :timers-list-with-date
  [{:keys [route-params]}]
  [::timers-page-navigated (date-from-route-params route-params)])

;; We need to do this only for timers-list-with-date since there's currently no way to
;; return to :timers-list from a :timers-list-with-date route. If there were a way to
;; transition in that direction, we would have to override the default behaviour there as
;; well.

(defmethod routing-events/on-pre-route-change-event
  :timers-list-with-date
  [_route {:keys [db]}]
  (if (not= :timers-list (:page-key db))
    {:db (db/clear-page-state db)}
    {}))

(defmethod routing-events/on-route-change-event
  :timers-list
  [{:keys [route-params]}]
  [::timers-page-navigated (time/current-calendar-date)])

(rf/reg-event-fx
  ::calendar-select-date
  (fn [{:keys [db]} [_ date]]
    (let [[year month day] (time/calendar-date->string-parts date)]
      {:history-token (routes/path-for :timers-list-with-date
                                       :year year
                                       :month month
                                       :day day
                                       :slug (org-ctx-db/current-organization-slug db))})))

(rf/reg-event-db
  ::select-date
  (fn [db [_ calendar-date]]
    (db/set-in-page-state db [:selected-date] calendar-date)))

(rf/reg-event-fx
  ::modify-selected-date
  (fn [{:keys [db]} [_ number-of-days]]
    (let [[year month day] (-> db
                               (db/get-in-page-state [:selected-date])
                               (time/modify-calendar-date number-of-days)
                               time/calendar-date->string-parts)]
      {:history-token (routes/path-for :timers-list-with-date
                                       :year year
                                       :month month
                                       :day day
                                       :slug (org-ctx-db/current-organization-slug db))})))

(rf/reg-event-fx
  ::timers-page-navigated
  (fn [{:keys [db]} [_ date]]
    {:db (db/set-page-key db :timers-list)
     :fx [[:dispatch [::org-invites-events/fetch-invited-orgs]]
          [:dispatch [::select-date date]]
          [:dispatch [::task-events/fetch-tasks (org-ctx-db/current-organization-slug db)]]
          [:dispatch [::timer-events/fetch-timers date]]]}))

(rf/reg-event-db
  ::show-create-timer-widget
  (fn [db _]
    (db/set-in-page-state db [:show-create-timer-widget] true)))

(rf/reg-event-db
  ::dismiss-create-timer-widget
  (fn [db _]
    (db/set-in-page-state db [:show-create-timer-widget] false)))

(defn- selected-date [db]
  (db/get-in-page-state db [:selected-date]))

(rf/reg-event-fx
  ::create-timer-succeeded
  (fn [{:keys [db]} _]
    {:fx [[:dispatch [::dismiss-create-timer-widget]]
          [:dispatch [::timer-events/fetch-timers (selected-date db)]]]}))

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
  (fn [{:keys [db]} [_ timer-id]]
    {:fx [[:http-xhrio (api/start-timer timer-id
                                        [::timer-events/fetch-timers (selected-date db)]
                                        [::flash-error (str "We couldn't start your timer "
                                                            (:frown config/emojis)
                                                            " Please try again!")])]]}))

(rf/reg-event-fx
  ::stop-timer
  (fn [{:keys [db]} [_ timer-id]]
    {:fx [[:http-xhrio (api/stop-timer timer-id
                                       [::timer-events/fetch-timers (selected-date db)]
                                       [::flash-error (str "We couldn't stop your timer "
                                                           (:frown config/emojis)
                                                           " Please try again!")])]]}))

(rf/reg-event-fx
  ::delete-timer
  (fn [{:keys [db]} [_ timer-id]]
    {:fx [[:http-xhrio (api/delete-timer timer-id
                                         [::timer-events/fetch-timers (selected-date db)]
                                         [::flash-error (str "We couldn't delete your timer "
                                                             (:frown config/emojis)
                                                             " Please try again!")])]]}))

(rf/reg-event-fx
  ::edit-timer-succeeded
  (fn [{:keys [db]} [_ timer-id]]
    {:fx [[:dispatch [::timer-events/fetch-timers (selected-date db)]]
          [:dispatch [::dismiss-edit-timer-modal timer-id]]]}))

(rf/reg-event-fx
  ::edit-timer-failed
  (fn [_ _]
    (flash-error-effect (str "We couldn't edit your timer "
                             (:frown config/emojis)
                             " Please try again!"))))

(defn- duration-string [timer]
  (let [{:keys [hours minutes]} (time/timer-duration timer (time/now))]
    (str hours ":" (ustring/left-pad 2 "0" (str minutes)))))

(rf/reg-event-fx
  ::show-edit-timer-modal
  (fn [{:keys [db]} [_ timer-id]]
    (let [timer (db-timers/timer-by-id db timer-id)]
      {:db (db/set-in-page-state db [:show-edit-timer-modal timer-id] true)
       ;; This is necessary because the modal content is not re-mounted
       ;; when closed and reopened.
       :fx [[:dispatch [::form/set-values
                        [::edit-timer-form timer-id]
                        (-> timer
                            (select-keys [:task-id :note])
                            (assoc :duration-in-hour-mins (duration-string timer)))]]]})))

(rf/reg-event-db
  ::dismiss-edit-timer-modal
  (fn [db [_ timer-id]]
    (db/set-in-page-state db [:show-edit-timer-modal timer-id] false)))
