(ns chronograph-web.utils.time
  (:require ["date-fns" :as date-fns])
  (:import [goog.date Date]))

(defn goog-date->calendar-date [date]
  {:day   (.getDate date)  ;; Between 1-31
   :month (.getMonth date) ;; Between 0-11
   :year  (.getYear date)})

(defn js-date->calendar-date [date]
  {:day   (.getDate date)  ;; Between 1-31
   :month (.getMonth date) ;; Between 0-11
   :year  (.getFullYear date)})

(defn- format-two-digits [n]
  (if (> n 9)
    (str n)
    (str "0" n)))

(defn calendar-date->string [{:keys [day month year]}]
  (str year "-" (format-two-digits (inc month)) "-" (format-two-digits day)))

(defn string->calendar-date [s]
  (goog-date->calendar-date (Date/fromIsoString s)))

(defn now []
  (js/Date.))

(defn current-calendar-date []
  (js-date->calendar-date (now)))

(defn string->date [s]
  (date-fns/parseISO s))

(defn- time-span-minutes [current-time {:keys [started-at stopped-at]}]
  (/ (date-fns/differenceInSeconds (or stopped-at current-time) started-at) 60))

(defn timer-duration [{:keys [time-spans]} current-time]
  (let [minutes (->> time-spans
                     (map (partial time-span-minutes current-time))
                     (reduce +)
                     (int))]
    {:minutes (rem minutes 60)
     :hours (quot minutes 60)}))
