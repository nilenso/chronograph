(ns chronograph.middleware
  (:require [taoensso.timbre :as log]
            [ring.util.response :as response]
            [chronograph.auth :as auth]
            [chronograph.domain.user :as user]
            [clojure.string :as string]))

(defn wrap-exception-logging [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (log/error e)
        (-> (response/response {:error "Internal Server Error"})
            (response/status 500))))))

(defn wrap-require-user-info
  [handler]
  (fn [{:keys [user] :as request}]
    (if-not user
      (-> (response/response {:error "Unauthorized"})
          (response/status 401))
      (handler request))))

(defn- token-from-bearer-value [bearer-value]
  (second (string/split bearer-value #" ")))

(defn- add-user-details [token request]
  (if-let [user (some-> token
                        auth/verify-token
                        :id
                        user/find-by-id)]
    (assoc request :user user)
    request))

(defn wrap-header-auth
  [handler]
  (fn [{:keys [headers] :as request}]
    (let [token (some-> headers (get "authorization") token-from-bearer-value)]
      (-> token
          (add-user-details request)
          handler))))

(defn wrap-cookie-auth
  [handler]
  (fn [{:keys [cookies] :as request}]
    (let [token (-> cookies (get "auth-token") :value)]
      (-> token
          (add-user-details request)
          handler))))

(def wrap-authenticated
  (comp wrap-header-auth
        wrap-cookie-auth
        wrap-require-user-info))
