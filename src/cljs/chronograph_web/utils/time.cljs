(ns chronograph-web.utils.time
  (:require ["date-fns" :as date-fns])
  (:import [goog.date Date DateTime]))

(defn goog-date->calendar-date [date]
  {:day   (.getDate date)  ;; Between 1-31
   :month (.getMonth date) ;; Between 0-11
   :year  (.getYear date)})

(defn js-date->calendar-date [date]
  {:day   (.getDate date)  ;; Between 1-31
   :month (.getMonth date) ;; Between 0-11
   :year  (.getFullYear date)})

(defn- format-day [day]
  (if (> day 9)
    (str day)
    (str "0" day)))

(defn calendar-date->string [{:keys [day month year]}]
  (str year "-" (inc month) "-" (format-day day)))

(defn string->calendar-date [s]
  (goog-date->calendar-date (Date/fromIsoString s)))

(defn current-calendar-date []
  (js-date->calendar-date (js/Date.)))

(defn string->date [s]
  (date-fns/parseISO s))

#_({:updated-at "2020-11-03T06:32:44.118225Z", :task-id 1, :recorded-for {:day 3, :month 10, :year 2020}, :task {:organization-id 1, :id 1, :updated-at "2020-10-28T06:53:36.068Z", :created-at "2020-10-28T06:53:36.068Z", :description "foo", :archived-at nil, :name "foo"}, :user-id 1, :note nil, :id #uuid "a991c780-4b55-4124-a216-9f902eee095c", :time-spans ({:started-at #inst "2020-11-03T13:22:33.000-00:00", :stopped-at #inst "2020-11-03T14:26:47.867-00:00"}), :created-at "2020-11-03T06:32:44.118225Z"} {:updated-at "2020-11-03T06:32:57.878239Z", :task-id 1, :recorded-for {:day 3, :month 10, :year 2020}, :task {:organization-id 1, :id 1, :updated-at "2020-10-28T06:53:36.068Z", :created-at "2020-10-28T06:53:36.068Z", :description "foo", :archived-at nil, :name "foo"}, :user-id 1, :note nil, :id #uuid "0e430642-9b57-475c-9a25-4bba1c03420e", :time-spans ({:started-at #inst "2020-11-03T14:22:33.000-00:00", :stopped-at #inst "2020-11-03T18:26:47.867-00:00"}), :created-at "2020-11-03T06:32:57.878239Z"})

(defn- time-span-minutes [current-time {:keys [started-at stopped-at]}]
  (date-fns/differenceInMinutes (or stopped-at current-time) started-at))

(defn timer-duration [{:keys [time-spans]} current-time]
  (let [minutes (->> time-spans
                     (map (partial time-span-minutes current-time))
                     (reduce +))]
    {:minutes (rem minutes 60)
     :hours (quot minutes 60)}))
