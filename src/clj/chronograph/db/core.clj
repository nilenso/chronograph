(ns chronograph.db.core
  (:require [mount.core :refer [defstate]]
            [next.jdbc :as jdbc]
            [next.jdbc.date-time :as jdbc-date-time]
            [next.jdbc.result-set :as jdbc-result-set]
            [next.jdbc.sql :as sql]
            [chronograph.config :as config]
            [chronograph.db.core :as db]
            [chronograph.utils.time :as time]
            [camel-snake-kebab.core :as csk]))

(jdbc-date-time/read-as-instant)

(defstate datasource
  :start (jdbc/get-datasource (:db-spec config/config))
  :stop nil)

(def sql-opts {:builder-fn jdbc-result-set/as-kebab-maps
               :column-fn  csk/->snake_case_string
               :table-fn   csk/->snake_case_string})

(defn where
  ([table-name tx attributes]
   (where table-name tx attributes {}))
  ([table-name tx attributes options]
   (sql/find-by-keys tx
                     table-name
                     attributes
                     (merge db/sql-opts options))))

(defn find-by [table-name tx attributes]
  (first (where table-name
                tx
                attributes
                {:limit 1})))

(defn update! [table-name tx attributes updates]
  (when
   (sql/update! tx
                table-name
                (merge {:updated-at (time/now)} updates)
                attributes
                db/sql-opts)
    (find-by table-name tx attributes)))

(defn create! [table-name tx attributes]
  (let [now (time/now)]
    (sql/insert! tx
                 table-name
                 (merge {:created-at now
                         :updated-at now}
                        attributes)
                 db/sql-opts)))
