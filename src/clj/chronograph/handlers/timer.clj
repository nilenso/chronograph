(ns chronograph.handlers.timer
  (:require [chronograph.domain.timer :as timer]
            [next.jdbc :as jdbc]
            [chronograph.db.core :as db]
            [chronograph.domain.task :as task]
            [chronograph.utils.coercions :as coerce]
            [ring.util.response :as response]
            [clojure.spec.alpha :as s]
            [chronograph.domain.acl :as acl]))

(defn create
  "Authorized users may create a timer for the give task id."
  [{{:keys [task-id note] :as body} :body
    {user-id :users/id} :user
    {organization-id :organizations/id} :organization
    :as _request}]
  (jdbc/with-transaction [tx db/datasource]
    (cond
      (not (s/valid? :handlers.timer/create-request-body body))
      (response/bad-request
       {:error "Invalid Task ID or Note."})

      (not (acl/belongs-to-org? tx user-id organization-id))
      (-> (response/response {:error "Forbidden."})
          (response/status 403))

      (empty? (task/find-by-id tx task-id))
      (response/bad-request
       {:error "Task does not exist."})

      :else (-> (timer/create! tx
                               organization-id
                               user-id
                               task-id
                               note)
                response/response))))

(defn delete
  "Authorized users may delete any timer they own."
  [{{:keys [timer-id]} :params
    {user-id :users/id} :user
    :as _request}]
  (let [timer-id (coerce/str-to-uuid timer-id)]
    (jdbc/with-transaction [tx db/datasource]
      (if (not (s/valid? :timers/id timer-id))
        (response/bad-request
         {:error "Invalid Timer ID."})
        (if-let [time-span (timer/delete! tx user-id timer-id)]
          (-> time-span
              response/response)
          (response/bad-request
           {:error "Timer does not exist."}))))))

(defn update-note
  "Authorized users may update note for a timer they own, for the give task id."
  [{{:keys [timer-id]} :params
    {:keys [note]} :body
    {user-id :users/id} :user
    :as _request}]
  (let [timer-id (coerce/str-to-uuid timer-id)]
    (jdbc/with-transaction [tx db/datasource]
      (cond
        (not (and (s/valid? :timers/id timer-id)
                  (s/valid? :timers/note note)))
        (response/bad-request
         {:error "Invalid Timer ID or Note."})

        (empty? (timer/find-for-user-by-timer-id tx user-id timer-id))
        (response/bad-request
         {:error "Timer does not exist."})

        :else (-> (timer/update-note! tx user-id timer-id note)
                  response/response)))))

(defn start
  "Authorized users may start any timer they own."
  [{{:keys [timer-id]} :params
    {user-id :users/id} :user
    :as _request}]
  (let [timer-id (coerce/str-to-uuid timer-id)]
    (jdbc/with-transaction [tx db/datasource]
      (cond
        (not (s/valid? :timers/id timer-id))
        (response/bad-request
         {:error "Invalid Timer ID."})

        (empty? (timer/find-for-user-by-timer-id tx user-id timer-id))
        (response/bad-request
         {:error "Timer does not exist."})

        :else (if-let [time-span (timer/start! tx user-id timer-id)]
                (-> time-span
                    response/response)
                (response/bad-request
                 {:error "Timer is already started."}))))))

(defn stop
  "Authorized users may stop any timer they own."
  [{{:keys [timer-id]} :params
    {user-id :users/id} :user
    :as _request}]
  (let [timer-id (coerce/str-to-uuid timer-id)]
    (jdbc/with-transaction [tx db/datasource]
      (cond
        (not (s/valid? :timers/id timer-id))
        (response/bad-request
         {:error "Invalid Timer ID."})

        (empty? (timer/find-for-user-by-timer-id tx user-id timer-id))
        (response/bad-request
         {:error "Timer does not exist."})

        :else (if-let [time-span (timer/stop! tx user-id timer-id)]
                (-> time-span
                    response/response)
                (response/bad-request
                 {:error "Timer is already stopped."}))))))

(defn find-for-user-task
  "Authorised users may look up all timers for any task they own."
  [{{:keys [task-id]} :params
    {user-id :users/id} :user
    :as _request}]
  (jdbc/with-transaction [tx db/datasource]
    (if (not (s/valid? :timers/task-id task-id))
      (response/bad-request
       {:error "Invalid Task ID."})
      (if-let [timer (timer/find-for-user-task tx user-id task-id)]
        (-> timer
            response/response)
        (response/not-found
         {:error "Timers not found."})))))
