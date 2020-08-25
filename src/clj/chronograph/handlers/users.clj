(ns chronograph.handlers.users
  (:require [chronograph.auth :as auth]
            [ring.util.response :as response]))

(defn me [{:keys [cookies] :as request}]
  (if-let [credentials (some-> cookies
                               (get "auth-token")
                               :value
                               auth/unsign-token)]
    (response/response (select-keys credentials [:id :email :name :provider]))
    (-> (response/response {:error "Unauthorized"})
        (response/status 401))))
