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
                  :description "Task description"}
            response (task/create {:body task
                                   :organization organization})
            retrieved-task (with-transaction [tx db/datasource]
                             (db-task/find-by tx {:id (:tasks/id (:body response))}))]
        (is (= 200 (:status response)))
        (is (= (:name task) (:tasks/name retrieved-task)))
        (is (= (:description task) (:tasks/description retrieved-task)))
        (is (nil? (:tasks/archived-at retrieved-task)))))

    (testing "Returns 400 if name is not present"
      (let [task {:description "Invalid task"}
            response (task/create {:body task})]
        (is (= 400 (:status response)))))

    (testing "Creates a task with an empty description"
      (let [task {:name "Task name"}
            response (task/create {:body task
                                   :organization organization})]
        (is (= 200 (:status response)))))))

(deftest index-tasks-test
  (let [user (factories/create-user)
        organization (factories/create-organization (:users/id user))
        _tasks (dotimes [_ 5] (factories/create-task organization))]

    (testing "Returns a list of tasks for the organization"
      (let [request {:organization organization}
            response (task/index request)]
        (is (= 200 (:status response)))
        (is (= 5 (-> response :body count)))))))

(deftest update-test
  (let [user (factories/create-user)
        organization (factories/create-organization (:users/id user))
        {task-id :tasks/id} (factories/create-task organization)
        now (time/now)]

    (testing "Returns 404 if the task is not found"
      (let [random-task-id (rand-int 10)
            request {:params {:task-id random-task-id}
                     :organization organization}
            response (task/update request)]
        (is (= 404 (:status response)))))

    (testing "It returns 200 and the updated task if updates are valid"
      (with-redefs [time/now (constantly now)]
        (let [updates {:name "Updated value" :description "Updated description"}
              request {:params {:task-id task-id}
                       :organization organization
                       :body {:updates updates}}
              response (task/update request)
              {:tasks/keys [name description updated-at]} (:body response)]
          (is (= 200 (:status response)))
          (is (= updates {:name name
                          :description description}))
          (is (= now updated-at)))))))

(deftest archive-test
  (let [user (factories/create-user)
        organization (factories/create-organization (:users/id user))
        {task-id :tasks/id} (factories/create-task organization)
        now (time/now)]

    (testing "Returns 404 if the task is not found"
      (let [random-task-id (rand-int 10)
            request {:params {:task-id random-task-id}
                     :organization organization}
            response (task/archive request)]
        (is (= 404 (:status response)))))

    (testing "It returns 200 and archives the tasks"
      (with-redefs [time/now (constantly now)]
        (let [request {:params {:task-id task-id}
                       :organization organization}
              response (task/archive request)
              {:tasks/keys [archived-at updated-at]} (:body response)]
          (is (= 200 (:status response)))
          (is (= now archived-at))
          (is (= now updated-at)))))))
