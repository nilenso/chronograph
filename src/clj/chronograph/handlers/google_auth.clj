(ns chronograph.handlers.google-auth
  (:require [ring.util.codec :as codec]
            [ring.util.response :as response]
            [chronograph.config :as config]
            [chronograph.auth :as auth]
            [chronograph.db.core :as db]
            [chronograph.domain.user :as user]
            [org.httpkit.client :as http]
            [mount.core :refer [defstate]]
            [next.jdbc :as jdbc]
            [cheshire.core :as json]
            [taoensso.timbre :as log])
  (:import (com.google.api.client.googleapis.auth.oauth2 GoogleIdTokenVerifier$Builder GoogleIdTokenVerifier)
           (com.google.api.client.googleapis.javanet GoogleNetHttpTransport)
           (com.google.api.client.json.jackson2 JacksonFactory)))

(defstate google-id-token-verifier
  :start (-> (GoogleIdTokenVerifier$Builder. (GoogleNetHttpTransport/newTrustedTransport)
                                             (JacksonFactory.))
             (.setAudience [(get-in config/config [:oauth :google :client-id])])
             (.build))
  :stop nil)

(defn verify-id-token
  [^String token]
  (some->> token
           (.verify ^GoogleIdTokenVerifier google-id-token-verifier)
           (.getPayload)
           (into {})))

(defn- auth-code-uri [{:keys [redirect-uri
                              client-id
                              response-type
                              scope
                              login-endpoint]}]
  (format "%s?redirect_uri=%s&client_id=%s&scope=%s&response_type=%s"
          login-endpoint
          (codec/url-encode redirect-uri)
          (codec/url-encode client-id)
          (codec/url-encode scope)
          (codec/url-encode response-type)))

(defn- redirect-uri [client-type]
  (let [{:keys [web-redirect-uri desktop-redirect-uri]} (get-in config/config
                                                                [:oauth :google])]
    (if (= "desktop" client-type)
      desktop-redirect-uri
      web-redirect-uri)))

(defn login-handler [request]
  (let [client-type (get-in request [:params :client-type])]
    (response/redirect (-> config/config
                           (get-in [:oauth :google])
                           (assoc :redirect-uri (redirect-uri client-type))
                           auth-code-uri))))

(defn- redirect-with-error
  ([error]
   (redirect-with-error "/" error))
  ([url-prefix error]
   (response/redirect (format "%s?%s=%s"
                              url-prefix
                              (codec/url-encode "auth-error")
                              (codec/url-encode error)))))

(defn- fetch-id-token [client-type auth-code]
  (let [{:keys [token-endpoint
                client-id
                client-secret]} (get-in config/config [:oauth :google])
        {:keys [status body error]} @(http/post {:uri token-endpoint
                                                 :form-params {"client_id"     client-id
                                                               "client_secret" client-secret
                                                               "code"          auth-code
                                                               "grant_type"    "authorization_code"
                                                               "redirect_uri"  (redirect-uri client-type)}})]
    (cond
      (some? error) (throw error)
      (not= 200 status) (throw (ex-info "Received an unexpected status code from Google's token endpoint"
                                        {:status status
                                         :body   body}))
      :else (-> body
                json/parse-string
                (get "id_token")))))

(defn web-redirect-handler [{:keys [params]}]
  (try
    (if (:error params)
      (redirect-with-error (:error params))
      (jdbc/with-transaction [tx db/datasource]
        (let [id-token (fetch-id-token "web" (:code params))
              {:strs [name sub email email_verified picture]} (verify-id-token id-token)]
          (if-not email_verified
            (redirect-with-error "email-not-verified")
            (let [{:users/keys [id]} (user/find-or-create-google-user! tx sub name email picture)]
              (-> (response/redirect "/")
                  (auth/set-auth-cookie id)))))))
    (catch Exception e
      ;; We can't bubble up the exception and rely on our middleware here,
      ;; because we need to render the error appropriately to the user.
      (log/error e)
      (redirect-with-error "unexpected-error"))))

(defn desktop-redirect-handler [{:keys [params]}]
  (let [url-prefix (str (get-in config/config [:auth :desktop-redirect-url-scheme])
                        "://")]
    (try
      (if (:error params)
        (redirect-with-error url-prefix (:error params))
        (jdbc/with-transaction [tx db/datasource]
          (let [id-token (fetch-id-token "desktop" (:code params))
                {:strs [name sub email email_verified picture]} (verify-id-token id-token)]
            (if-not email_verified
              (redirect-with-error url-prefix "email-not-verified")
              (let [{:users/keys [id]} (user/find-or-create-google-user! tx sub name email picture)
                    access-token (auth/create-token id)]
                (response/redirect (format "%s?access-token=%s"
                                           url-prefix
                                           access-token)))))))
      (catch Exception e
        ;; We can't bubble up the exception and rely on our middleware here,
        ;; because we need to render the error appropriately to the user.
        (log/error e)
        (redirect-with-error url-prefix "unexpected-error")))))
