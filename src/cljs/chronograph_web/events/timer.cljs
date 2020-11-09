(ns chronograph-web.events.timer
  (:require [chronograph-web.utils.time :as time]
            [chronograph-web.api-client :as api]
            [chronograph-web.db.timers :as db-timers]
            [chronograph-web.db.tasks :as db-tasks]
            [re-frame.core :as rf]
            [medley.core :as medley]
            [chronograph-web.config :as config]))

(rf/reg-event-fx
  ::fetch-timers
  (fn [_ [_ calendar-date]]
    {:fx [[:http-xhrio (api/fetch-timers calendar-date
                                         [::fetch-timers-success calendar-date]
                                         [::fetch-timers-fail calendar-date])]]}))

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
      (update :created-at ->date-time)
      (update :updated-at ->date-time)
      (update :id uuid)
      (dissoc :task)))

(rf/reg-event-db
  ::fetch-timers-success
  (fn [db [_ calendar-date {:keys [timers] :as _response}]]
    (-> db
        (db-timers/set-timers calendar-date (map convert-timer timers))
        (db-tasks/merge-tasks (map :task timers)))))

(rf/reg-event-fx
  ::fetch-timers-fail
  (fn [_ _]
    {:flash-error {:content (str "Oh no, something went wrong! "
                                 (:frown config/emojis)
                                 " Please refresh the page!")}}))
