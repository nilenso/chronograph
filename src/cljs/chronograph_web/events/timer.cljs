(ns chronograph-web.events.timer
  (:require [chronograph-web.utils.time :as time]
            [chronograph-web.api-client :as api]
            [chronograph-web.db.timers :as db-timers]
            [chronograph-web.db.tasks :as db-tasks]
            [re-frame.core :as rf]
            [medley.core :as medley]
            [chronograph-web.config :as config])
  (:import [goog.date Date DateTime]))

(rf/reg-event-fx
  ::fetch-timers
  (fn [_ [_ day]]
    ;; TODO: accept day as a calendar-date
    {:fx [[:http-xhrio (api/fetch-timers day
                                         [::fetch-timers-success day]
                                         [::fetch-timers-fail day])]]}))

(defn- ->date [s]
  (when s
    (time/string->calendar-date s)))

(defn- ->date-time [s]
  (when s
    (time/string->date s)))

(defn- convert-timer [m]
  (-> m
      (update :recorded-for ->date)
      (update :time-spans (fn [ts] (map #(medley/map-vals ->date-time %) ts)))
      (update :id uuid)
      (dissoc :task)))

(rf/reg-event-db
  ::fetch-timers-success
  (fn [db [_ date-str {:keys [timers] :as _response}]]
    (-> db
        (db-timers/set-timers (->date date-str) (map convert-timer timers))
        (db-tasks/merge-tasks (map :task timers)))))

(rf/reg-event-fx
  ::fetch-timers-fail
  (fn [_ _]
    {:flash-error {:content (str "Oh no, something went wrong! "
                                 (:frown config/emojis)
                                 " Please refresh the page!")}}))