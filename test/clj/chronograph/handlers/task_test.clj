(ns chronograph.handlers.task-test
  (:require [clojure.test :refer :all]
            [chronograph.factories :as factories]
            [chronograph.handlers.task :as task]
            [chronograph.db.task :as db-task]
            [chronograph.fixtures :as fixtures]))

(use-fixtures :once fixtures/config fixtures/datasource)
(use-fixtures :each fixtures/clear-db)

(deftest create-a-task-test
  (let [user (factories/create-user)
        organization (factories/create-organization (:users/id user)) ]
    (testing "Given a name and description, it creates a task"
      (let [task {:name "Task name"
                  :description "Task description"
                  :organization-id (:organizations/id organization)}
            response (task/create {:body task})
            retrieved-task (db-task/find-by-id (:tasks/id (:body response)))]
        (is (= 200 (:status response)))
        (is (= (:name task) (:tasks/name retrieved-task)))
        (is (= (:description task) (:tasks/description retrieved-task)))
        (is (= nil (:tasks/archived-at retrieved-task)))))
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
  (testing "Returns a list of tasks for the organization"
    (let [user (factories/create-user)
          organization (factories/create-organization (:users/id user))
          _tasks (dotimes [_ 5] (factories/create-task (:organizations/id organization)))
          request {:params {:organization-id (:organizations/id organization)}}
          response (task/index request)]
      (is (= 200 (:status response)))
      (is (= 5 (-> response :body count))))))
