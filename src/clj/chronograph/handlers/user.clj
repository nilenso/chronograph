(ns chronograph.handlers.user
  (:require [chronograph.auth :as auth]
            [ring.util.response :as response]
            [chronograph.db.user :as users-db]))

(defn me [{:keys [cookies] :as request}]
  (if-let [user (some-> cookies
                        (get "auth-token")
                        :value
                        auth/verify-token
                        :id
                        users-db/find-by-id)]
    (response/response user)
    (-> (response/response {:error "Unauthorized"})
        (response/status 401))))
