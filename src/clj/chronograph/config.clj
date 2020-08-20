(ns chronograph.config
  (:require [aero.core :as aero]
            [mount.core :as mount :refer [defstate]]))

(defstate config
  :start (let [config-file (get-in (mount/args) [:options :config-file])]
           (aero/read-config config-file))
  :stop nil)
