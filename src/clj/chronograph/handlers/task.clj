(ns chronograph.handlers.task
  (:require [clojure.spec.alpha :as s]
            [chronograph.domain.task :as task]
            [ring.util.response :as response]))

(defn create [{:keys [body] :as request}]
  (let [{:keys [name description]} body ]
    (if-not (s/valid? :tasks/create-params-handler body)
      (response/bad-request {:error "Name should be present"})
      (-> (task/create name description)
          (response/response)))))
