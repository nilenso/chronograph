(ns chronograph.core
  (:gen-class)
  (:require [taoensso.timbre :as log]
            [mount.core :as mount]
            [mount-up.core :as mu]
            [chronograph.cli :as cli]))

(defn- log-mount-action [action-map]
  (fn [{:keys [name action]}]
    (log/info {:event (action-map action)
               :state name})))

(defn mount-init! []
  (mu/all-clear)
  (mu/on-upndown :before-info
                 (log-mount-action {:up ::state-up-pre
                                    :down ::state-down-pre})
                 :before)
  (mu/on-upndown :after-info
                 (log-mount-action {:up ::state-up-post
                                    :down ::state-down-post})
                 :after)
  (mu/on-up :around-exceptions
            (mu/try-catch
              (fn [ex {:keys [name]}] (log/error ex {:event     ::state-up-failure
                                                     :state     name
                                                     :exception ex})))
            :wrap-in))

(defn -main
  [& args]
  (mount-init!)
  (let [opts (cli/parse args)]
    (if-let [opts-error (cli/error-message opts)]
      (do
        (print opts-error)
        (System/exit 1))
      (case (cli/operational-mode opts)
        :help (print (cli/help-message opts))
        :serve (mount/start-with-args opts)
        (print (cli/help-message opts))))))
