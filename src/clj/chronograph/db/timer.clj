(ns chronograph.db.timer
  (:require [next.jdbc.sql :as sql]
            [chronograph.db.core :as db]
            [chronograph.utils.time :as time]
            [next.jdbc :as jdbc]
            [medley.core :as medley]
            [clojure.spec.alpha :as s])
  (:import (java.time Instant)))

(defn- ->instant
  [s]
  (when s
    (Instant/parse s)))

(defn- coerce-time-span
  [{:keys [started-at stopped-at]}]
  {:started-at (->instant started-at)
   :stopped-at (->instant stopped-at)})

(defn- coerce-time-spans-in-timer
  [timer]
  (medley/update-existing timer
                          :timers/time-spans
                          (partial map coerce-time-span)))

(defn create! [tx attrs]
  (-> (db/create! :timers tx (merge {:timers/time-spans []} attrs))
      coerce-time-spans-in-timer))

(defn delete!
  "Given a timer ID, deletes the timer.

   - MUST ensure user isolation."
  [tx user-id timer-id]
  (-> (jdbc/execute-one! tx
                         ["DELETE FROM timers WHERE user_id = ? AND id = ?"
                          user-id
                          timer-id]
                         db/sql-opts)
      coerce-time-spans-in-timer))

(defn update!
  [tx timer-id update-fields]
  (-> (db/update! :timers tx {:id timer-id} update-fields)
      coerce-time-spans-in-timer))

(defn find-by-id
  [tx timer-id]
  (some-> (db/find-by :timers tx {:id timer-id})
          coerce-time-spans-in-timer))

(defn find-by-user-and-id
  [tx user-id timer-id]
  (-> (jdbc/execute-one! tx
                         ["SELECT * FROM timers WHERE user_id = ? AND id =?"
                          user-id timer-id]
                         db/sql-opts)
      coerce-time-spans-in-timer))

(defn find-by-user-and-task
  [tx user-id task-id]
  (->> (sql/find-by-keys tx
                         :timers
                         {:user-id user-id
                          :task-id task-id}
                         db/sql-opts)
       (mapv coerce-time-spans-in-timer)))

(defn- extract-task [m]
  (-> (medley/remove-keys #(= "tasks" (namespace %)) m)
      (assoc :task (medley/filter-keys #(= "tasks" (namespace %)) m))))

(defn find-by-user-and-recorded-for
  [tx user-id recorded-for]
  (->> (db/query tx
                 ["SELECT timers.*, tasks.* AS task
                  FROM tasks, timers
                  WHERE tasks.id = timers.task_id 
                  AND user_id = ?
                  AND recorded_for = ?"
                  user-id
                  recorded-for])
       (map extract-task)))

(defn add-time-span
  [tx timer-id time-span]
  {:pre [(s/valid? :timers/time-span time-span)]}
  (let [now (time/now)]
    (-> (jdbc/execute-one! tx
                           ["UPDATE timers SET
                             time_spans = time_spans || ?,
                             updated_at = ?
                             WHERE id = ?"
                            [time-span]
                            now
                            timer-id]
                           db/sql-opts)
        coerce-time-spans-in-timer)))

(defn set-stopped-at
  "Sets stopped-at on the last time span of a timer."
  [tx timer-id stopped-at]
  (let [now (time/now)]
    (-> (jdbc/execute-one! tx
                           ["UPDATE timers SET
                             time_spans = jsonb_set(time_spans, '{-1, stopped-at}', (? :: jsonb)),
                             updated_at = ?
                             WHERE id = ?"
                            (format "\"%s\"" stopped-at)
                            now
                            timer-id]
                           db/sql-opts)
        coerce-time-spans-in-timer)))

(defn find-running-timers
  [tx user-id]
  (->> (db/query tx ["SELECT * FROM timers
                      WHERE (time_spans #> '{-1, \"stopped-at\"}') = 'null'::jsonb
                      AND   user_id = ?" user-id])
       (mapv coerce-time-spans-in-timer)))
