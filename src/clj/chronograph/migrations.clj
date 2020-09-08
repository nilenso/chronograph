(ns chronograph.migrations
  (:require [chronograph.config :as config]
            [migratus.core :as migratus]))

(defn config []
  {:store :database
   :db    (:db-spec config/config)})

(defn migrate []
  (migratus/migrate (config)))

(defn rollback []
  (migratus/rollback (config)))
