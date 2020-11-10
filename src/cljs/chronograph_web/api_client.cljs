(ns chronograph-web.api-client
  (:require [chronograph-web.http :as http]
            [chronograph-web.config :as config]
            [chronograph-web.utils.time :as time]))

(defn fetch-organizations
  [on-success on-failure]
  (http/get {:uri        (str config/api-root "/organizations/")
             :on-success on-success
             :on-failure on-failure}))

(defn fetch-profile
  [on-success on-failure]
  (http/get {:uri        (str config/api-root "/users/me")
             :on-success on-success
             :on-failure on-failure}))

(defn fetch-timers
  [day on-success on-failure]
  (http/get {:uri        (str config/api-root "/timers")
             :params     {:day (time/calendar-date->string day)}
             :on-success on-success
             :on-failure on-failure}))

(defn fetch-tasks [slug on-success on-failure]
  (http/get {:uri        (str config/api-root "/organizations/" slug "/tasks")
             :on-success on-success
             :on-failure on-failure}))

(defn archive-task [slug task-id]
  (http/put {:uri        (str config/api-root
                              "/organizations/"
                              slug
                              "/tasks/"
                              task-id
                              "/archive")
             :on-success [::archive-task-success slug task-id]
             :on-failure [::archive-task-failure]}))

(defn create-and-start-timer
  [task-id note recorded-for on-success on-failure]
  (http/post {:uri        (str config/api-root "/timers")
              :params     {:task-id      task-id
                           :note         note
                           :recorded-for (time/calendar-date->string recorded-for)}
              :on-success on-success
              :on-failure on-failure}))

(defn start-timer
  [timer-id on-success on-failure]
  (http/put {:uri        (str config/api-root "/timers/" timer-id "/start")
             :on-success on-success
             :on-failure on-failure}))

(defn stop-timer
  [timer-id on-success on-failure]
  (http/put {:uri        (str config/api-root "/timers/" timer-id "/stop")
             :on-success on-success
             :on-failure on-failure}))
