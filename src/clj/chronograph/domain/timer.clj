(ns chronograph.domain.timer
  (:require [chronograph.db.timer :as db-timer]
            [chronograph.domain.task :as task]
            [chronograph.domain.acl :as acl]
            [chronograph.utils.time :as time]
            [chronograph.domain.timer.time-span :as time-span]))

(defn create!
  "Create a timer for a task IFF the user and the task, both belong to
  the given organization. If the note is nil, write an empty string into the DB."
  [tx organization-id {:timers/keys [user-id task-id] :as timer}]
  (when (and (acl/belongs-to-org? tx user-id organization-id)
             (= organization-id
                (:tasks/organization-id
                 (task/find-by-id tx task-id))))
    (db-timer/create! tx (-> timer
                             (update :timers/note #(or % ""))))))

(defn delete!
  "Given a timer ID for a user, delete it if it exists and also delete
  all its time spans.

  Return timer id if the delete is successful, nil otherwise.

  MUST get called in a transaction."
  [tx user-id timer-id]
  (db-timer/delete! tx
                    user-id
                    timer-id))

(defn running?
  "Given a user id and timer id, return truthy if the timer has a running span
  associated with it. Return nil if there are no running spans."
  [{:timers/keys [time-spans] :as _timer}]
  (and (not-empty time-spans)
       (not (:stopped-at (last time-spans)))))

(defn update!
  [tx timer-id {:keys [duration-in-secs] :as update-params}]
  (when-let [{:timers/keys [time-spans]} (db-timer/find-by-id tx timer-id)]
    (db-timer/update! tx timer-id (if duration-in-secs
                                    (-> update-params
                                        (assoc :time-spans (time-span/adjust-time-spans-to-duration time-spans duration-in-secs))
                                        (dissoc :duration-in-secs))
                                    update-params))))

(defn belongs-to-user?
  "Given a user id and timer id, return truthy if the timer belongs to the
  user. Otherwise, return nil."
  [tx user-id timer-id]
  (db-timer/find-by-user-and-id tx user-id timer-id))

(defn- started-time-span []
  {:started-at (time/now)
   :stopped-at nil})

(defn stop!
  "Given a timer ID for a user, stop the timer and return the stopped time span.
  If the timer is not running, do nothing and return nil."
  [tx user-id timer-id]
  (when-let [timer-to-start (db-timer/find-by-user-and-id tx user-id timer-id)]
    (when (running? timer-to-start)
      (db-timer/set-stopped-at tx timer-id (time/now)))))

(defn- stop-running-timers!
  [tx user-id]
  (doseq [{:timers/keys [id]} (db-timer/find-running-timers tx user-id)]
    (stop! tx user-id id)))

(defn start!
  "Given a timer ID for a user, start it and return the started time span.
  If the timer is already running, do nothing and return nil.
  Other running timers will be stopped."
  [tx user-id timer-id]
  (when-let [timer-to-start (db-timer/find-by-user-and-id tx user-id timer-id)]
    (when-not (running? timer-to-start)
      (stop-running-timers! tx user-id)
      (db-timer/add-time-span tx timer-id (started-time-span)))))

(defn create-and-start!
  "Creates and starts a timer."
  [tx organization-id {:timers/keys [user-id] :as timer}]
  (when-let [{:timers/keys [id]} (create! tx organization-id timer)]
    (start! tx user-id id)))

(defn find-by-user-and-id
  "Given a timer id and a user id, return a consolidated timer object,
  with timer data and all the associated time spans. Return nil if no timer
  is found."
  [tx user-id timer-id]
  (when-let [timer (db-timer/find-by-user-and-id tx user-id timer-id)]
    timer))

(defn find-by-user-and-task
  "Given a user's id and a task id, find all the related timers. Each timer
  has timer information as well as time span information. Return nil if no
  timers are found."
  [tx user-id task-id]
  (when-let [timers (not-empty (db-timer/find-by-user-and-task tx user-id task-id))]
    timers))

(defn find-by-user-and-recorded-for
  "Given a user's id and a date, find all of that user's timers for that
  date. Return [] if no timers are found."
  [tx user-id recorded-for]
  (db-timer/find-by-user-and-recorded-for tx user-id recorded-for))

