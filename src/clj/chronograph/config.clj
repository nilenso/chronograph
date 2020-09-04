(ns chronograph.config
  (:require [aero.core :as aero]
            [clojure.spec.alpha :as s]
            [mount.core :as mount :refer [defstate]]))

(s/def :db-spec/dbtype #{"postgresql"})
(s/def :db-spec/dbname string?)
(s/def :db-spec/host string?)
(s/def :db-spec/port int?)
(s/def :db-spec/user string?)
(s/def :db-spec/password string?)

(s/def ::db-spec
  (s/keys :req-un
          [:db-spec/dbtype
           :db-spec/dbname
           :db-spec/host
           :db-spec/port
           :db-spec/user
           :db-spec/password]))

(s/def :google/web-redirect-uri string?)
(s/def :google/response-type #{"code"})
(s/def :google/login-endpoint string?)
(s/def :google/token-endpoint string?)
(s/def :google/client-id string?)
(s/def :google/client-secret string?)
(s/def :google/scope string?)

(s/def :oauth/google
  (s/keys :req-un
          [:google/web-redirect-uri
           :google/response-type
           :google/login-endpoint
           :google/token-endpoint
           :google/client-id
           :google/client-secret
           :google/scope]))

(s/def ::oauth (s/keys :req-un [:oauth/google]))

(s/def :auth/token-signing-key string?)
(s/def :auth/token-expiry-in-seconds int?)

(s/def ::auth
  (s/keys :req-un
          [:auth/token-signing-key
           :auth/token-expiry-in-seconds]))

(s/def ::port int?)

(s/def ::config (s/keys :req-un [::db-spec ::oauth ::auth ::port]) )

(defstate config
  :start (let [config-file (get-in (mount/args) [:options :config-file])
               config (aero/read-config config-file)]
           (if (s/valid? ::config config)
             config
             (throw (ex-info "Failed to validate config" {}))))
  :stop nil)
