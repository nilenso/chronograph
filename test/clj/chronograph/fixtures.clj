(ns chronograph.fixtures
  (:require [chronograph.config :as config]
            [chronograph.specs]
            [mount.core :as mount]
            [chronograph.db.core :as db]
            [next.jdbc :as jdbc]
            [taoensso.timbre :as log]))

(defn config [f]
  (mount/stop #'config/config)
  (mount/start-with-args {:options {:config-file (or (System/getenv "TEST_CONFIG_FILE")
                                                     "config/config.test.edn")}}
                         #'config/config)
  (f)
  (mount/stop #'config/config))

(defn datasource [f]
  (mount/stop #'db/datasource)
  (mount/start #'db/datasource)
  (f)
  (mount/stop #'db/datasource))

(defn truncate-all-tables! []
  (jdbc/execute! db/datasource ["TRUNCATE TABLE google_profiles CASCADE"])
  (jdbc/execute! db/datasource ["TRUNCATE TABLE users CASCADE"])
  (jdbc/execute! db/datasource ["TRUNCATE TABLE acls CASCADE"])
  (jdbc/execute! db/datasource ["TRUNCATE TABLE organizations CASCADE"]))

(defn clear-db [f]
  (truncate-all-tables!)
  (f))

(defn report-logging-only [f]
  (log/with-level :report (f)))
