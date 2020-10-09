(ns dev.repl-utils
  (:require [mount.core :as mount]
            [chronograph.server :as server]
            [chronograph.core :as core]
            [chronograph.config :as config]
            [chronograph.migrations :as migrations]
            [migratus.core :as migratus]
            [chronograph.db.core :as db]))

(defn start-app! []
  (core/mount-init!)
  (-> (mount/with-args {:options {:config-file "config/config.dev.edn"}})
      (mount/swap-states {#'server/server {:start #(server/start-server! #'server/handler)
                                           :stop  #(server/server)}})
      mount/start))

(defn restart-app! []
  (mount/stop)
  (start-app!))

(defn load-config!
  ([] (load-config! "config/config.dev.edn"))
  ([config-file]
   (mount/stop #'config/config)
   (mount/start-with-args {:options {:config-file config-file}} #'config/config)))

(defn create-migration [migration-name]
  (migratus/create (migrations/config) migration-name))

(defn remove-user-from-org!
  [user-id org-id]
  (db/delete! :acls
              db/datasource
              {:user-id         user-id
               :organization-id org-id}))
