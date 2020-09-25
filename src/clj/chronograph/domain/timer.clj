(ns chronograph.domain.timer
  (:require [chronograph.db.timer :as db-timer]
            [chronograph.db.time-span :as db-time-span]))

;; Timer state transitions
#_{:create [:start]
   :start [:running] ; create TimeSpan on start
   :running [:stop]  ; update running TimeSpan on stop
   :stop [:start]    ; create new TimeSpan on restart
   }

(defn create!
  [tx user-id task-id note]
  ;; TODO: Create timer IFF task id exists, so we don't barf with PG exception.
  (db-timer/create! tx
                    user-id
                    task-id
                    (or note "")))

(defn delete!
  "Given a timer ID for a user, delete it if it exists and also delete
  all its time spans.

  Return timer id if the delete is successful, nil otherwise.

  MUST get called in a transaction."
  [tx user-id timer-id]
  (db-timer/delete! tx
                    user-id
                    timer-id))

(defn update-note!
  "Update the note in the given user's timer.

  Return the timer object if successful, or nil when the timer does not exist."
  [tx user-id timer-id note]
  (db-timer/update-note! tx user-id timer-id note))

(defn- running-span
  "Return running span if any, falsey otherwise.

  The DB guarantees that at most one span is 'running' for any timer.
  This span is the running span."
  [tx user-id timer-id]
  (db-time-span/find-running-span-for-user-by-timer-id tx
                                                       user-id
                                                       timer-id))

(defn running?
  "Given a user id and timer id, return truthy if the timer has a running span
  associated with it. Return nil if there are no running spans."
  [tx user-id timer-id]
  (seq (running-span tx user-id timer-id)))

(defn belongs-to-user?
  "Given a user id and timer id, return truthy if the timer belongs to the
  user. Otherwise, return nil."
  [tx user-id timer-id]
  (db-timer/find-for-user-by-timer-id tx user-id timer-id))

(defn start!
  "Given a timer ID for a user, start it and return the started time span.
  If the timer is already running, do nothing and return nil."
  [tx user-id timer-id]
  (when (and (belongs-to-user? tx user-id timer-id)
             (not (running? tx user-id timer-id)))
    (dissoc (db-time-span/create! tx timer-id)
            :time-spans/created-at
            :time-spans/updated-at)))

(defn stop!
  "Given a timer ID for a user, stop the timer and return the stopped time span.
  If the timer is not running, do nothing and return nil."
  [tx user-id timer-id]
  (when (belongs-to-user? tx user-id timer-id)
    (when-let [time-span (running-span tx user-id timer-id)]
      (dissoc (db-time-span/update-stop-time! tx
                                              (:time-spans/id time-span))
              :time-spans/created-at
              :time-spans/updated-at))))

(defn- assoc-time-spans
  "Given a timer, look up all the time spans for the timer and return
  the timer object with associated collection of time spans."
  [tx {:timers/keys [id] :as timer}]
  (assoc timer
         :time-spans (db-time-span/find-all-for-timer tx id)))

(defn find-for-user-by-timer-id
  "Given a timer id and a user id, return a consolidated timer object,
  with timer data and all the associated time spans. Return nil if no timer
  is found."
  [tx user-id timer-id]
  (when-let [timer (db-timer/find-for-user-by-timer-id tx user-id timer-id)]
    (assoc-time-spans tx
                      timer)))

(defn find-for-user-task
  "Given a user's id and a task id, find all the related timers. Each timer
  has timer information as well as time span information. Return nil if no
  timers are found."
  [tx user-id task-id]
  (when-let [timers (not-empty (db-timer/find-for-user-by-task-id tx user-id task-id))]
    (mapv (partial assoc-time-spans tx)
          timers)))
