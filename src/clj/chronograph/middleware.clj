(ns chronograph.middleware
  (:require [chronograph.auth :as auth]
            [chronograph.db.user :as users-db]
            [ring.util.response :as response]
            [taoensso.timbre :as log]))

(defn wrap-exception-logging [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (log/error e)
        (-> (response/response {:error "Internal Server Error"})
            (response/status 500))))))


(defn wrap-authenticated-user
  [handler]
  (fn [{:keys [cookies] :as request}]
    (let [user (some-> cookies
                       (get "auth-token")
                       :value
                       auth/verify-token
                       :id
                       users-db/find-by-id)]
      (-> request
          (assoc :user user)
          handler))))
