(ns chronograph.server
  (:require [mount.core :refer [defstate]]
            [bidi.ring :as bidi]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [taoensso.timbre :as log]
            [org.httpkit.server :as httpkit]
            [ring.util.response :as response]
            [chronograph.config :as config]
            [chronograph.middleware :as middleware]
            [chronograph.handlers.google-auth :as google-auth]
            [chronograph.handlers.user :as user]))

(def routes ["/" [["" (constantly (-> (response/resource-response "public/index.html")
                                      (response/content-type "text/html")))]
                  ["google-login" google-auth/login-handler]
                  ["google-oauth2-redirect" google-auth/oauth2-redirect-handler]
                  ["api/" [["users/me" {:get user/me}]]]
                  [true (constantly (-> (response/response "Not Found")
                                        (response/status 404)))]]])

(def handler
  (-> routes
      bidi/make-handler
      (middleware/wrap-exception-logging)
      (wrap-json-response)
      (wrap-json-body {:keywords? true :bigdecimals? true})
      (wrap-keyword-params)
      (wrap-params)
      (wrap-cookies)
      (wrap-resource "public")
      (wrap-content-type)
      (wrap-not-modified)))

(defn start-server!
  ([] (start-server! handler))
  ([app-handler]
   (log/info {:event ::server-start})
   (httpkit/run-server app-handler
                       {:port (:port config/config)})))

(defstate server
  :start (start-server!)
  :stop (server))
