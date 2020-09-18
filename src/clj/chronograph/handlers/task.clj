(ns chronograph.handlers.task
  (:require [clojure.spec.alpha :as s]
            [chronograph.domain.task :as task]
            [ring.util.response :as response]))

(defn create [{:keys [body] :as _request}]
  (if-not (s/valid? :tasks/create-params-handler body)
    (response/bad-request {:error "Name should be present"})
    (-> (task/create (select-keys body [:name :description :organization-id]))
        (response/response))))

(defn index [{:keys [params]}]
  (if-let [organization-id (:organization-id params)]
    (-> {:tasks (task/index {:organization-id organization-id})}
        (response/response))
    (response/bad-request {:error "Organization ID is necessary"})))
