(ns chronograph-web.events.tasks
  (:require [chronograph-web.api-client :as api]
            [chronograph-web.config :as config]
            [chronograph-web.db.tasks :as db-tasks]
            [re-frame.core :as rf]))

(rf/reg-event-fx
  ::fetch-tasks
  (fn [_ [_ slug]]
    {:http-xhrio (api/fetch-tasks slug [::fetch-tasks-success] [::fetch-tasks-failure])}))

(rf/reg-event-db
  ::fetch-tasks-success
  (fn [db [_ tasks]]
    (db-tasks/merge-tasks db tasks)))

(rf/reg-event-fx
  ::fetch-tasks-failure
  (fn [_ _]
    {:flash-error {:content (str "Oh no, something went wrong! "
                                 (:frown config/emojis)
                                 " Please refresh the page!")}}))
