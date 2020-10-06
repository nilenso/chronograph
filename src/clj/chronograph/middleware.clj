(ns chronograph.middleware
  (:require [taoensso.timbre :as log]
            [ring.util.response :as response]
            [chronograph.auth :as auth]
            [chronograph.db.core :as db]
            [chronograph.domain.acl :as acl]
            [chronograph.domain.user :as user]
            [chronograph.domain.organization :as organization]
            [clojure.string :as string]
            [next.jdbc :as jdbc]))

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
  (jdbc/with-transaction [tx db/datasource]
    (if-let [user (some->> token
                           auth/verify-token
                           :id
                           (user/find-by-id tx))]
      (assoc request :user user)
      request)))

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

(defn wrap-current-organization
  [handler]
  (fn [{:keys [params] :as request}]
    (when-let [organization (jdbc/with-transaction [tx db/datasource]
                              (organization/find-by
                               tx
                               {:slug (:slug params)}))]
      (->> organization
           (assoc request :organization)
           handler))))

(defn wrap-require-organization
  [handler]
  (fn [{:keys [organization] :as request}]
    (if organization
      (handler request)
      (response/not-found {:error "Organization not found"}))))

(defn wrap-user-authorization
  [handler roles]
  (fn [{:keys [user organization] :as request}]
    (jdbc/with-transaction [tx db/datasource]
      (let [user-role  (acl/role tx (:users/id user) (:organizations/id organization))]
        (if ((set roles) user-role)
          (handler request)
          (-> (response/response {:error "Forbidden"})
              (response/status 403)))))))

(defn wrap-require-authorization [handler roles]
  (-> handler
      (wrap-user-authorization roles)
      wrap-require-organization
      wrap-current-organization
      wrap-authenticated))
