(ns chronograph-web.http
  (:refer-clojure :exclude [get])
  (:require [ajax.core :as ajax]))

(defn get
  [uri http-xhrio-map]
  (merge {:method :get
          :uri uri
          :timeout 8000 ;; TODO: put in config?
          :response-format (ajax/json-response-format {:keywords? true})}
         http-xhrio-map))


(defn post [uri http-xhrio-map]
  (merge {:method :post
          :uri uri
          :format          (ajax/json-request-format)
          :response-format (ajax/json-response-format {:keywords? true})}
         http-xhrio-map))
