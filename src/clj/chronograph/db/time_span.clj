(ns chronograph.db.time-span
  (:require [next.jdbc.sql :as sql]
            [chronograph.db.core :as db]
            [chronograph.utils.time :as time]
            [next.jdbc :as jdbc]))

(defn create!
  [tx timer-id]
  (let [now (time/now)]
    (sql/insert! tx
                 :time-spans
                 {:timer-id timer-id
                  :started-at now
                  :created-at now
                  :updated-at now}
                 db/sql-opts)))

(defn update-stop-time!
  [tx time-span-id]
  (let [now (time/now)]
    (sql/update! tx
                 :time-spans
                 {:stopped-at now}
                 {:id time-span-id}
                 (assoc db/sql-opts
                        :return-keys true))))

(defn find-by-id
  [tx time-span-id]
  (sql/get-by-id tx
                 :time-spans
                 time-span-id
                 db/sql-opts))

(defn find-all-for-timer
  [tx timer-id]
  (db/query tx
            ["SELECT id, timer_id, started_at, stopped_at
               FROM time_spans
               WHERE timer_id = ?"
             timer-id]))

(defn find-running-span-for-user-by-timer-id
  [tx user-id timer-id]
  (jdbc/execute-one! tx
                     ["SELECT s.id, s.timer_id, s.started_at, s.stopped_at
                       FROM time_spans s, timers t
                       WHERE s.stopped_at is NULL
                             AND s.timer_id = t.id
                             AND t.user_id = ?
                             AND t.id = ?"
                      user-id
                      timer-id]
                     db/sql-opts))
