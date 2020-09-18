(ns chronograph-web.http
  (:refer-clojure :exclude [get])
  (:require [ajax.core :as ajax]
            [chronograph-web.config :as config]))

(defn get
  [uri http-xhrio-map]
  (merge {:method :get
          :uri uri
          :timeout config/request-timeout
          :response-format (ajax/json-response-format {:keywords? true})}
         http-xhrio-map))

(defn post [uri http-xhrio-map]
  (merge {:method :post
          :uri uri
          :format          (ajax/json-request-format)
          :response-format (ajax/json-response-format {:keywords? true})}
         http-xhrio-map))
