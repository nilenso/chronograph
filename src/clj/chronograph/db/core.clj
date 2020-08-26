(ns chronograph.db.core
  (:require [mount.core :refer [defstate]]
            [next.jdbc :as jdbc]
            [next.jdbc.date-time :as jdbc-date-time]
            [chronograph.config :as config]))

(jdbc-date-time/read-as-instant)

(defstate datasource
  :start (jdbc/get-datasource {:jdbcUrl (:db-connection-string config/config)})
  :stop nil)
