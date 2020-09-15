(ns chronograph.handlers.task-test
  (:require [clojure.test :refer :all]
            [chronograph.handlers.task :as task]
            [chronograph.db.task :as db-task]
            [chronograph.fixtures :as fixtures]))

(use-fixtures :once fixtures/config fixtures/datasource)
(use-fixtures :each fixtures/clear-db)

(deftest create-a-task
  (testing "Given a name and description, it creates a task"
    (let [task {:name "Task name"
                :description "Task description"}
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
    (let [task {:name "Task name"}
          response (task/create {:body task})]
      (is (= 200 (:status response))))))
