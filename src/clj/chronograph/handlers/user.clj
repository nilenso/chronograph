(ns chronograph.handlers.user
  (:require [ring.util.response :as response]))

(defn me [request]
  (response/response (:user request)))
