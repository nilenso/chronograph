(ns dev.repl-utils
  (:require [mount.core :as mount]
            [chronograph.server :as server]
            [chronograph.core :as core]))

(defn start-app! []
  (core/mount-init!)
  (-> (mount/with-args {:options {:config-file "config/config.dev.edn"}})
      (mount/swap-states {#'server/server {:start #(server/start-server! #'server/handler)
                                           :stop #(server/server)}})
      mount/start))

(defn restart-app! []
  (mount/stop)
  (start-app!))
