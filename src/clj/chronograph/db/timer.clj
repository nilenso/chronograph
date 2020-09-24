(ns chronograph.db.timer
  (:require [next.jdbc.sql :as sql]
            [chronograph.db.core :as db]
            [chronograph.utils.time :as time]
            [next.jdbc :as jdbc]))

(defn create!
  [tx user-id task-id note]
  (let [now (time/now)]
    (sql/insert! tx
                 :timers
                 {:user-id user-id
                  :task-id task-id
                  :note note
                  :created-at now
                  :updated-at now}
                 db/sql-opts)))

(defn delete!
  "Given a timer ID, delete all of its time_spans and then delete the timer.

   - MUST get called in a transaction
   - MUST ensure user isolation."
  [tx user-id timer-id]
  (jdbc/execute-one! tx
                     ["DELETE FROM time_spans s
                       USING timers t
                       WHERE t.user_id = ? AND s.timer_id = ?"
                      user-id
                      timer-id]
                     db/sql-opts)
  (jdbc/execute-one! tx
                     ["DELETE FROM timers WHERE user_id = ? AND id = ?"
                      user-id
                      timer-id]
                     db/sql-opts))

(defn find-for-user-by-timer-id
  [tx user-id timer-id]
  (jdbc/execute-one! tx
                     ["SELECT * FROM timers WHERE user_id = ? AND id =?"
                      user-id timer-id]
                     db/sql-opts))

(defn find-for-user-by-task-id
  [tx user-id task-id]
  (sql/find-by-keys tx
                    :timers
                    {:user-id user-id
                     :task-id task-id}
                    db/sql-opts))

(defn update-note!
  [tx user-id timer-id note]
  (let [now (time/now)]
    (sql/update! tx
                 :timers
                 {:note note
                  :updated-at now}
                 {:id timer-id
                  :user-id user-id}
                 (assoc db/sql-opts
                        :return-keys true))))

(comment
  (do
    (require 'chronograph.factories)
    (require 'chronograph.specs)
    (require 'clojure.spec.alpha)

    (def ^:private user (chronograph.factories/create-user))
    (def ^:private org (chronograph.factories/create-organization (:users/id user)))
    (def ^:private task (chronograph.factories/create-task (:organizations/id org)))

    (def ^:private timer (create! db/datasource
                                  (:users/id user)
                                  (:tasks/id task)
                                  "A sample note.")))

  (find-for-user-by-timer-id db/datasource
                             (:users/id user)
                             (:timers/id timer))

  (clojure.spec.alpha/valid? :timers/timer
                             (find-for-user-by-timer-id db/datasource
                                                        (:users/id user)
                                                        (:timers/id timer)))

  (find-for-user-by-task-id db/datasource
                            (:users/id user)
                            (:tasks/id task))

  (every? (partial clojure.spec.alpha/valid? :timers/timer)
          (find-for-user-by-task-id db/datasource
                                    (:users/id user)
                                    (:tasks/id user)))

  (update-note! db/datasource
                (:users/id user)
                (:timers/id timer)
                (str "A note was added " (rand-int Integer/MAX_VALUE))))
