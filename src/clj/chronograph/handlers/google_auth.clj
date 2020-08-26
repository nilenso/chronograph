(ns chronograph.handlers.google-auth
  (:require [ring.util.codec :as codec]
            [ring.util.response :as response]
            [chronograph.config :as config]
            [chronograph.auth :as auth]
            [org.httpkit.client :as http]
            [mount.core :refer [defstate]]
            [cheshire.core :as json])
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

(defn oauth2-redirect-handler [{:keys [params] :as request}]
  ;; TODO: Handle an error response from Google
  (let [{:keys [token-endpoint
                client-id
                client-secret
                redirect-uri]} (get-in config/config [:oauth :google])
        token-response @(http/post token-endpoint
                                   {:form-params {"client_id"     client-id
                                                  "client_secret" client-secret
                                                  "code"          (:code params)
                                                  "grant_type"    "authorization_code"
                                                  "redirect_uri"  redirect-uri}})
        id-token       (some-> token-response
                               :body
                               json/parse-string
                               (get "id_token"))
        {:strs [name sub email email_verified]} (token->credentials id-token)
        ;; TODO: check if email is verified, if not return an error
        ;; TODO: check if user is in the DB, if not create them
        ]
    (-> (response/redirect "/")
        (auth/set-auth-cookie sub email name "google"))))
