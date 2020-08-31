(ns chronograph.migrations
  (:require [chronograph.config :as config]
            [migratus.core :as migratus]))

(defn config []
  {:store :database
   :db    {:connection-uri (:db-connection-string config/config)}})

(defn migrate []
  (migratus/migrate (config)))

(defn rollback []
  (migratus/rollback (config)))
