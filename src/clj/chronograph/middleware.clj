(ns chronograph.middleware
  (:require [taoensso.timbre :as log]
            [ring.util.response :as response]))

(defn wrap-exception-logging [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (log/error e)
        (-> (response/response {:error "Internal Server Error"})
            (response/status 500))))))
