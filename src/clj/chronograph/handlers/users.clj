(ns chronograph.handlers.users
  (:require [chronograph.auth :as auth]
            [ring.util.response :as response]
            [chronograph.db.users :as users-db]))

(defn me [{:keys [cookies] :as request}]
  (if-let [user (some-> cookies
                        (get "auth-token")
                        :value
                        auth/unsign-token
                        :id
                        users-db/find-by-id)]
    (response/response user)
    (-> (response/response {:error "Unauthorized"})
        (response/status 401))))
