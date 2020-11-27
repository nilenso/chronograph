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

(defn archive-task [slug task-id on-success on-failure]
  (http/put {:uri        (str config/api-root
                              "/organizations/"
                              slug
                              "/tasks/"
                              task-id
                              "/archive")
             :on-success on-success
             :on-failure on-failure}))

(defn- tasks-uri [slug]
  (str "/api/organizations/" slug "/tasks/"))

(defn create-task [org-slug name description on-success on-failure]
  (http/post {:uri        (tasks-uri org-slug)
              :params     {:name        name
                           :description description}
              :on-success on-success
              :on-failure on-failure}))

(defn update-task [org-slug task-id updates on-success on-failure]
  (http/put {:uri        (str (tasks-uri org-slug)
                              task-id)
             :params     {:updates updates}
             :on-success on-success
             :on-failure on-failure}))

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

(defn delete-timer
  [timer-id on-success on-failure]
  (http/delete {:uri        (str config/api-root "/timers/" timer-id)
                :on-success on-success
                :on-failure on-failure}))

(defn reject-invite
  [org-slug on-success on-failure]
  (http/post {:uri        (str "/api/invitations/" org-slug "/reject")
              :on-success on-success
              :on-failure on-failure}))

(defn accept-invite
  [org-slug on-success on-failure]
  (http/post {:uri        (str "/api/invitations/" org-slug "/accept")
              :on-success on-success
              :on-failure on-failure}))

(defn fetch-invited-orgs
  [on-success on-failure]
  (http/get {:uri        "/api/invitations"
             :on-success on-success
             :on-failure on-failure}))
