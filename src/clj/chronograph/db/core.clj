(ns chronograph.db.core
  (:require [mount.core :refer [defstate]]
            [next.jdbc :as jdbc]
            [next.jdbc.date-time :as jdbc-date-time]
            [next.jdbc.result-set :as jdbc-result-set]
            [next.jdbc.sql :as sql]
            [chronograph.config :as config]
            [chronograph.utils.time :as time]
            [camel-snake-kebab.core :as csk]))

(jdbc-date-time/read-as-instant)

(extend-protocol jdbc-result-set/ReadableColumn
  java.sql.Date
  (read-column-by-label [^java.sql.Date v _] (.toLocalDate v))
  (read-column-by-index [^java.sql.Date v _1 _2] (.toLocalDate v)))

(defstate datasource
  :start (jdbc/get-datasource (:db-spec config/config))
  :stop nil)

(def sql-opts {:builder-fn jdbc-result-set/as-kebab-maps
               :column-fn  csk/->snake_case_string
               :table-fn   csk/->snake_case_string
               :return-keys true})

(defn where
  ([table-name tx attributes]
   (where table-name tx attributes {}))
  ([table-name tx attributes options]
   (sql/find-by-keys tx
                     table-name
                     attributes
                     (merge sql-opts options))))

(defn find-by [table-name tx attributes]
  (first (where table-name
                tx
                attributes
                {:limit 1})))

(defn update! [table-name tx attributes updates]
  (sql/update! tx
               table-name
               (merge updates
                      {:updated-at (time/now)})
               attributes
               sql-opts))

(defn create! [table-name tx attributes]
  (let [now (time/now)]
    (sql/insert! tx
                 table-name
                 (merge attributes
                        {:created-at now
                         :updated-at now})
                 sql-opts)))

(defn delete! [table-name tx attributes]
  (sql/delete! tx table-name attributes sql-opts))

(defn query [tx sql-vec]
  (sql/query tx sql-vec sql-opts))
