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
      (response/not-found :not-found))))

;; TODO: Same as create, check if anyone can update a task
(defn update [{:keys [params body] :as request}]
  (jdbc/with-transaction [tx db/datasource]
    (if-let [organization (req-utils/current-organization tx request)]
      (let [id (Integer/parseInt (str (:task-id params)))]
        (task/update tx {:tasks/id id} (:updates body))
        (if-let [task (task/find-by-id tx id)]
          {:status 200
           :body task}
          (response/not-found "Task not found")))
      (response/not-found "Organization not found"))))

(defn archive [{:keys [params] :as request}]
  (jdbc/with-transaction [tx db/datasource]
    (if-let [organization (req-utils/current-organization tx request)]
      (let [id (Integer/parseInt (str (:task-id params)))]
        (task/archive tx {:tasks/id id})
        (if-let [task (task/find-by-id tx id)]
          {:status 200
           :body task}
          (response/not-found "Task not found")))
      (response/not-found "Organization not found"))))
