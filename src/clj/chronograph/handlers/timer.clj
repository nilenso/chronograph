(ns chronograph.handlers.timer
  (:refer-clojure :exclude [update])
  (:require [chronograph.domain.timer :as timer]
            [next.jdbc :as jdbc]
            [chronograph.db.core :as db]
            [chronograph.domain.task :as task]
            [chronograph.utils.coercions :as coerce]
            [ring.util.response :as response]
            [clojure.spec.alpha :as s]
            [chronograph.domain.acl :as acl])
  (:import [java.time LocalDate]))

(s/def :handlers.timer/recorded-for string?)
(s/def :handlers.timer/create-request-body (s/keys :req-un [:timers/task-id :handlers.timer/recorded-for]
                                                   :opt-un [:timers/note]))

(defn create
  "Authorized users may create a timer for the give task id."
  [{{:keys [task-id note recorded-for] :as body} :body
    {user-id :users/id}                          :user
    :as                                          _request}]
  (jdbc/with-transaction [tx db/datasource]
    (if-not (s/valid? :handlers.timer/create-request-body body)
      (response/bad-request {:error "Invalid Task ID or Note."})
      (if-let [{:tasks/keys [organization-id]} (task/find-by-id tx task-id)]
        (if-not (acl/belongs-to-org? tx user-id organization-id)
          (-> (response/response {:error "Forbidden."})
              (response/status 403))
          (-> (timer/create-and-start! tx
                                       organization-id
                                       #:timers{:user-id      user-id
                                                :task-id      task-id
                                                :recorded-for (LocalDate/parse recorded-for)
                                                :note         note})
              response/response))

        (response/bad-request {:error "Task does not exist."})))))

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
        (if-let [deleted-timer (timer/delete! tx user-id timer-id)]
          (-> deleted-timer
              response/response)
          (response/bad-request
           {:error "Timer does not exist."}))))))

(defn- coerce-update-body
  [body]
  (select-keys body [:task-id :note :duration-in-secs]))

(s/def :handlers.timer/duration-in-secs (some-fn pos-int?
                                                 zero?))
(s/def :handlers.timer/update-request-body (s/keys :opt-un [:timers/task-id
                                                            :timers/note
                                                            :handlers.timer/duration-in-secs]))

(defn- validate-update-params
  [tx user-id timer-id {:keys [task-id] :as update-params}]
  (cond
    (not (s/valid? :timers/id timer-id)) "Invalid timer ID."
    (not (s/valid? :handlers.timer/update-request-body update-params)) "Invalid request body."
    :else (if-let [timer (timer/find-by-user-and-id tx user-id timer-id)]
            (if (and task-id
                     (not (contains? (task/task-ids-of-organization tx (:timers/task-id timer))
                                     task-id)))
              "Task not found."
              nil)
            "Timer not found.")))

(defn update
  [{{:keys [timer-id]}  :params
    body                :body
    {user-id :users/id} :user}]
  (let [timer-id      (coerce/str-to-uuid timer-id)
        update-params (coerce-update-body body)]
    (jdbc/with-transaction [tx db/datasource]
      (if-let [error-message (validate-update-params tx user-id timer-id update-params)]
        (response/bad-request {:error error-message})
        (-> (timer/update! tx timer-id update-params)
            response/response)))))

(defn start
  "Authorized users may start any timer they own."
  [{{:keys [timer-id]} :params
    {user-id :users/id} :user
    :as _request}]
  (let [timer-id (coerce/str-to-uuid timer-id)]
    (jdbc/with-transaction [tx db/datasource]
      (cond
        (not (s/valid? :timers/id timer-id)) (response/bad-request
                                              {:error "Invalid Timer ID."})

        (empty? (timer/find-by-user-and-id tx
                                           user-id
                                           timer-id)) (response/bad-request
                                                       {:error "Timer does not exist."})

        :else (if-let [started-timer (timer/start! tx user-id timer-id)]
                (-> started-timer
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
        (not (s/valid? :timers/id timer-id)) (response/bad-request
                                              {:error "Invalid Timer ID."})

        (empty? (timer/find-by-user-and-id tx
                                           user-id
                                           timer-id)) (response/bad-request
                                                       {:error "Timer does not exist."})

        :else (if-let [stopped-timer (timer/stop! tx user-id timer-id)]
                (-> stopped-timer
                    response/response)
                (response/bad-request
                 {:error "Timer is already stopped."}))))))

(defn find-by-user-and-task
  "Authorised users may look up all timers for any task they own."
  [{{:keys [task-id]} :params
    {user-id :users/id} :user
    :as _request}]
  (jdbc/with-transaction [tx db/datasource]
    (if (not (s/valid? :timers/task-id task-id))
      (response/bad-request
       {:error "Invalid Task ID."})
      (if-let [timer (timer/find-by-user-and-task tx user-id task-id)]
        (-> timer
            response/response)
        (response/not-found
         {:error "Timers not found."})))))

(defn find-by-day [{{:keys [day]} :params
                    {user-id :users/id} :user
                    :as _request}]
  (jdbc/with-transaction [tx db/datasource]
    (response/response {:timers
                        (timer/find-by-user-and-recorded-for tx user-id (LocalDate/parse day))})))
