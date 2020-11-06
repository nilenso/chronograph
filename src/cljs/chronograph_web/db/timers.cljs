(ns chronograph-web.db.timers
  (:require [chronograph-web.db.tasks :as tasks-db]))

(defn timers-by-date
  [db date]
  (get-in db [:timers date]))

(defn set-timers [db date timers]
  (assoc-in db [:timers date] timers))

(defn timers-with-tasks
  [db date organization-id]
  (->> (timers-by-date db date)
       (map #(assoc % :task (tasks-db/find-by-id db (:task-id %))))
       (filter #(= organization-id (get-in % [:task :organization-id])))))
