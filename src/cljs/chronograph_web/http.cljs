(ns chronograph-web.http
  (:refer-clojure :exclude [get])
  (:require [ajax.core :as ajax]
            [chronograph-web.config :as config]))

(defn request [http-xhrio-map]
  (merge {:format          (ajax/json-request-format)
          :timeout config/request-timeout
          :response-format (ajax/json-response-format {:keywords? true})}
         http-xhrio-map))

(defn get [http-xhrio-map]
  (request (merge {:method :get
                   :format nil}
                  http-xhrio-map)))

(defn post [http-xhrio-map]
  (request (merge {:method :post} http-xhrio-map)))

(defn put [http-xhrio-map]
  (request (merge {:method :put} http-xhrio-map)))

(defn delete [http-xhrio-map]
  (request (merge {:method :delete} http-xhrio-map)))
