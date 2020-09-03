(ns chronograph.handlers.user
  (:require [ring.util.response :as response]))

(defn me [{:keys [user] :as request}]
  (if-not user
    (-> (response/response {:error "Unauthorized"})
        (response/status 401))
    (response/response user)))
