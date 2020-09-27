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
        ;; TODO: Can anyone create a task?
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

;; TODO: Same as create, check if anyone can update a task
(defn update [{:keys [params body] :as _request}]
  (let [id (Integer/parseInt (:task-id params))]
    (jdbc/with-transaction [tx db/datasource]
      (task/update tx {:tasks/id id} (:updates body))
      (if-let [task (task/find-by-id tx id)]
        {:status 200
         :body {:task task}}
        (response/not-found "Task not found")))))

(defn archive [{:keys [params] :as _request}]
  (let [id (Integer/parseInt (:task-id params))]
    (jdbc/with-transaction [tx db/datasource]
      (task/archive tx {:tasks/id id})
      (if-let [task (task/find-by-id tx id)]
        {:status 200
         :body {:task task}}
        (response/not-found "Task not found")))))
