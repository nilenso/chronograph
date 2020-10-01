(ns chronograph.handlers.task-test
  (:require [clojure.test :refer :all]
            [chronograph.factories :as factories]
            [chronograph.handlers.task :as task]
            [chronograph.db.core :as db]
            [chronograph.db.task :as db-task]
            [chronograph.fixtures :as fixtures]
            [chronograph.utils.time :as time]
            [next.jdbc :refer [with-transaction]]))

(use-fixtures :once fixtures/config fixtures/datasource)
(use-fixtures :each fixtures/clear-db)

(deftest create-a-task-test
  (let [user (factories/create-user)
        organization (factories/create-organization (:users/id user))]
    (testing "Given a name and description, it creates a task"
      (let [task {:name "Task name"
                  :description "Task description"
                  :organization-id (:organizations/id organization)}
            response (task/create {:body task})
            retrieved-task (with-transaction [tx db/datasource]
                             (db-task/find-by-id tx (:tasks/id (:body response))))]
        (is (= 200 (:status response)))
        (is (= (:name task) (:tasks/name retrieved-task)))
        (is (= (:description task) (:tasks/description retrieved-task)))
        (is (nil? (:tasks/archived-at retrieved-task)))))
    (testing "Returns 400 if name is not present"
      (let [task {:description "Invalid task"}
            response (task/create {:body task})]
        (is (= 400 (:status response)))))
    (testing "Creates a task with an empty description"
      (let [task {:name "Task name"
                  :organization-id (:organizations/id organization)}
            response (task/create {:body task})]
        (is (= 200 (:status response)))))))

(deftest index-tasks-test
  (let [user (factories/create-user)
        {organization-id :organizations/id} (factories/create-organization (:users/id user))
        _tasks (dotimes [_ 5] (factories/create-task organization-id))]
    (testing "Returns a list of tasks for the organization"
      (let [request {:params {:organization-id organization-id}}
            response (task/index request)]
        (is (= 200 (:status response)))
        (is (= 5 (-> response :body count)))))
    (testing "Returns 404 if the organization is not found"
      (let [random-org-id (rand-int 10)
            request {:params {:organization-id random-org-id}}
            response (task/index request)]
        (is (= 404 (:status response)))))))

(deftest update-test
  (let [user (factories/create-user)
        {organization-id :organizations/id} (factories/create-organization (:users/id user))
        {task-id :tasks/id} (factories/create-task organization-id)
        now (time/now)]
    (testing "Returns 404 if the organization is not found"
      (let [random-org-id (rand-int 10) ;; TODO: Change this random value
            request {:params {:organization-id random-org-id}}
            response (task/update request)]
        (is (= 404 (:status response)))))
    (testing "Returns 404 if the task is not found"
      (let [random-task-id (rand-int 10)
            request {:params {:organization-id organization-id
                              :task-id random-task-id}}
            response (task/update request)]
        (is (= 404 (:status response)))))
    (testing "It returns 200 and the updated task if updates are valid"
      ;; TODO: generate the updates
      (with-redefs [time/now (constantly now)]
        (let [updates {:name "Updated value" :description "Updated description"}
              request {:params {:organization-id organization-id
                                :task-id task-id}
                       :body {:updates updates}}
              response (task/update request)
              {:tasks/keys [name description updated-at]} (:body response)]
          (is (= 200 (:status response)))
          (is (= updates {:name name
                          :description description}))
          (is (= now updated-at)))))))

(deftest archive-test
  (let [user (factories/create-user)
        {organization-id :organizations/id} (factories/create-organization (:users/id user))
        {task-id :tasks/id} (factories/create-task organization-id)
        now (time/now)]
    (testing "Returns 404 if the organization is not found"
      (let [random-org-id (rand-int 10) ;; TODO: Change this random value
            request {:params {:organization-id random-org-id}}
            response (task/archive request)]
        (is (= 404 (:status response)))))
    (testing "Returns 404 if the task is not found"
      (let [random-task-id (rand-int 10)
            request {:params {:organization-id organization-id
                              :task-id random-task-id}}
            response (task/archive request)]
        (is (= 404 (:status response)))))
    (testing "It returns 200 and archives the tasks"
      (with-redefs [time/now (constantly now)]
        (let [request {:params {:organization-id organization-id
                                :task-id task-id}}
              response (task/archive request)
              {:tasks/keys [archived-at updated-at]} (:body response)]
          (is (= 200 (:status response)))
          (is (= now archived-at))
          (is (= now updated-at)))))))
