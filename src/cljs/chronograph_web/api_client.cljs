(ns chronograph-web.api-client
  (:require [chronograph-web.http :as http]
            [chronograph-web.config :as config]))

(defn fetch-organizations
  [on-success on-failure]
  (http/get {:uri        (str config/api-root "/organizations/")
             :on-success on-success
             :on-failure on-failure}))

(defn fetch-profile
  [on-success on-failure]
  (http/get {:uri (str config/api-root "/users/me")
             :on-success on-success
             :on-failure on-failure}))

(defn fetch-timers
  [day on-success on-failure]
  (http/get {:uri (str config/api-root "/timers")
             :method :get
             :params {:day day}
             :on-success on-success
             :on-failure on-failure}))
