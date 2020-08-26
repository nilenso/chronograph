(ns chronograph.fixtures
  (:require [chronograph.config :as config]
            [mount.core :as mount]))

(defn config [f]
  (mount/stop #'config/config)
  (mount/start-with-args {:options {:config-file "config/config.test.edn"}} #'config/config)
  (f)
  (mount/stop #'config/config))
