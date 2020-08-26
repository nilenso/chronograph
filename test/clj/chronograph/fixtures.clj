(ns chronograph.fixtures
  (:require [chronograph.config :as config]
            [mount.core :as mount]
            [chronograph.db.core :as db]
            [next.jdbc :as jdbc]))

(defn config [f]
  (mount/stop #'config/config)
  (mount/start-with-args {:options {:config-file "config/config.test.edn"}} #'config/config)
  (f)
  (mount/stop #'config/config))

(defn datasource [f]
  (mount/stop #'db/datasource)
  (mount/start #'db/datasource)
  (f)
  (mount/stop #'db/datasource))

(defn truncate-all-tables! []
  (jdbc/execute! db/datasource ["TRUNCATE TABLE google_profiles"])
  (jdbc/execute! db/datasource ["TRUNCATE TABLE linked_profiles"])
  (jdbc/execute! db/datasource ["TRUNCATE TABLE users CASCADE"]))

(defn clear-db [f]
  (truncate-all-tables!)
  (f))
