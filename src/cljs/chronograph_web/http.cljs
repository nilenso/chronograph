(ns chronograph-web.http
  (:require [ajax.core :as ajax]))

(defn post [uri m]
  (merge {:method :post
          :uri uri
          :format          (ajax/json-request-format)
          :response-format (ajax/json-response-format {:keywords? true})}
         m))
