(ns chronograph.auth
  (:require [buddy.sign.jwt :as jwt]
            [chronograph.config :as config]
            [taoensso.timbre :as log]
            [ring.util.response :as response]
            [chronograph.utils.time :as time])
  (:import (java.time.temporal ChronoUnit)))

(defn- secret-key []
  (get-in config/config [:auth :token-signing-key]))

(defn create-token [id]
  (jwt/sign {:id  id
             :exp (-> (time/now)
                      (.plus (get-in config/config [:auth :token-expiry-in-seconds])
                             ChronoUnit/SECONDS)
                      (.getEpochSecond))}
            (secret-key)))

(defn verify-token [token]
  (try
    (jwt/unsign token (secret-key))
    (catch Exception e
      (log/error e "Verifying a token failed")
      nil)))

(defn set-auth-cookie
  [response id]
  (response/set-cookie response
                       "auth-token"
                       (create-token id)
                       {:http-only true
                        ;; TODO: Set Secure in staging/prod
                        :same-site :strict
                        :max-age   (- (get-in config/config [:auth :token-expiry-in-seconds])
                                      100)}))
