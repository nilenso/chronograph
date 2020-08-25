(ns chronograph.auth
  (:require [buddy.sign.jwt :as jwt]
            [chronograph.config :as config]
            [taoensso.timbre :as log])
  (:import (java.time Instant)
           (java.time.temporal ChronoUnit)))

(defn- secret-key []
  (get-in config/config [:auth :token-signing-key]))

(defn create-token [id email name provider]
  (jwt/sign {:id       id
             :email    email
             :name     name
             :provider provider
             :exp      (-> (Instant/now)
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
