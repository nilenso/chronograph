(ns chronograph.handlers.task
  (:require [clojure.spec.alpha :as s]
            [chronograph.domain.task :as task]
            [chronograph.domain.organization :as organization]
            [chronograph.db.core :as db]
            [chronograph.utils.request :as req-utils]
            [ring.util.response :as response]
            [next.jdbc :as jdbc]))

(defn create [{:keys [body params] :as request}]
  (if-not (s/valid? :tasks/create-params-handler body)
    (response/bad-request {:error "Name should be present"})
    (jdbc/with-transaction [tx db/datasource]
      (when-let [organization (req-utils/current-organization tx request)]
        (-> (task/create tx {:name (:name body)
                             :description (:description body)
                             :organization-id (:organizations/id organization)})
            (response/response))))))

(defn index [request]
  (jdbc/with-transaction [tx db/datasource]
    (if-let [organization (req-utils/current-organization tx request)]
      (-> (organization/tasks tx organization)
          (response/response))
      (response/bad-request {:error "Organization slug is necessary"}))))
