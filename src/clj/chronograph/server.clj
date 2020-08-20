(ns chronograph.server
  (:require [mount.core :refer [defstate]]
            [bidi.ring :as bidi]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [taoensso.timbre :as log]
            [org.httpkit.server :as httpkit]
            [ring.util.response :as response]
            [chronograph.config :as config]))

(def routes ["/" [["" (constantly (-> (response/resource-response "public/index.html")
                                      (response/content-type "text/html")))]
                  [true (constantly (-> (response/response "Not Found")
                                        (response/status 404)))]]])

(def handler
  (-> routes
      bidi/make-handler
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
