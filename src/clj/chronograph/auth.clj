(ns chronograph.auth
  (:require [buddy.sign.jwt :as jwt]
            [chronograph.config :as config]
            [taoensso.timbre :as log]
            [ring.util.response :as response])
  (:import (java.time Instant)
           (java.time.temporal ChronoUnit)))

(defn- secret-key []
  (get-in config/config [:auth :token-signing-key]))

(defn create-token [id email name photo-url]
  (jwt/sign {:id        id
             :email     email
             :name      name
             :photo-url photo-url
             :exp       (-> (Instant/now)
                            (.plus (get-in config/config [:auth :token-expiry-in-seconds])
                                   ChronoUnit/SECONDS)
                            (.getEpochSecond))}
            (secret-key)))

(defn unsign-token [token]
  (try
    (jwt/unsign token (secret-key))
    (catch Exception e
      (log/error e "Unsigning a token failed")
      nil)))

(defn set-auth-cookie
  [response id email name photo-url]
  (response/set-cookie response
                       "auth-token"
                       (create-token id email name photo-url)
                       {:http-only true
                        ;; TODO: Set Secure in staging/prod
                        :same-site :strict
                        :max-age   (- (get-in config/config [:auth :token-expiry-in-seconds])
                                      100)}))
