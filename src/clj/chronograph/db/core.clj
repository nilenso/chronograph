(ns chronograph.db.core
  (:require [mount.core :refer [defstate]]
            [next.jdbc :as jdbc]
            [next.jdbc.date-time :as jdbc-date-time]
            [next.jdbc.result-set :as jdbc-result-set]
            [chronograph.config :as config]
            [camel-snake-kebab.core :as csk]))

(jdbc-date-time/read-as-instant)

(defstate datasource
  :start (jdbc/get-datasource (:db-spec config/config))
  :stop nil)

(def sql-opts {:builder-fn jdbc-result-set/as-unqualified-kebab-maps
               :column-fn  csk/->snake_case_string
               :table-fn   csk/->snake_case_string})
