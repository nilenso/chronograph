(ns chronograph.handlers.google-auth
  (:require [ring.util.codec :as codec]
            [ring.util.response :as response]
            [chronograph.config :as config]
            [chronograph.auth :as auth]
            [chronograph.db.users :as users-db]
            [org.httpkit.client :as http]
            [mount.core :refer [defstate]]
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

(defn token->credentials
  [^String token]
  (some->> token
           (.verify ^GoogleIdTokenVerifier google-id-token-verifier)
           (.getPayload)
           (into {})))

(defn- redirect-uri [{:keys [redirect-uri
                             client-id
                             response-type
                             scope
                             login-endpoint] :as oauth-config}]
  (format "%s?redirect_uri=%s&client_id=%s&scope=%s&response_type=%s"
          login-endpoint
          (codec/url-encode redirect-uri)
          (codec/url-encode client-id)
          (codec/url-encode scope)
          (codec/url-encode response-type)))

(defn login-handler [request]
  (response/redirect (-> config/config
                         (get-in [:oauth :google])
                         redirect-uri)))

(defn- redirect-with-error [error]
  (response/redirect (format "/?%s=%s"
                             (codec/url-encode "auth-error")
                             (codec/url-encode error))))

(defn- fetch-id-token [auth-code]
  (let [{:keys [token-endpoint
                client-id
                client-secret
                redirect-uri]} (get-in config/config [:oauth :google])
        {:keys [status body]} @(http/post token-endpoint
                                          {:form-params {"client_id"     client-id
                                                         "client_secret" client-secret
                                                         "code"          auth-code
                                                         "grant_type"    "authorization_code"
                                                         "redirect_uri"  redirect-uri}})]
    (if-not (= 200 status)
      (throw (ex-info "Received an unexpected status code from Google's token endpoint"
                      {:status status
                       :body   body}))
      (-> body
          json/parse-string
          (get "id_token")))))

(defn oauth2-redirect-handler [{:keys [params] :as request}]
  (try
    (if (:error params)
      (redirect-with-error (:error params))
      (let [id-token (fetch-id-token (:code params))
            {:strs [name sub email email_verified picture]} (token->credentials id-token)]
        (if-not email_verified
          (redirect-with-error "email-not-verified")
          (let [{:keys [id name email photo-url]} (users-db/find-or-create-google-user! sub name email picture)]
            (-> (response/redirect "/")
                (auth/set-auth-cookie id email name photo-url))))))
    (catch Exception e
      ;; We can't bubble up the exception and rely on our middleware here,
      ;; because we need to render the error appropriately to the user.
      (log/error e)
      (redirect-with-error "unexpected-error"))))
