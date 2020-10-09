(ns chronograph.domain.task-test
  (:require [clojure.test :refer :all]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [chronograph.db.core :as db]
            [chronograph.db.task :as db-task]
            [chronograph.domain.task :as task]
            [chronograph.factories :as factories]
            [chronograph.fixtures :as fixtures]
            [chronograph.utils.time :as time]
            [next.jdbc :refer [with-transaction]]))

(use-fixtures :once fixtures/config fixtures/datasource)
(use-fixtures :each fixtures/clear-db)

(deftest create-task-test
  (let [{user-id :users/id} (factories/create-user)
        organization (factories/create-organization user-id)]
    (testing "Can create task with name and description"
      (with-redefs [time/now (constantly (time/now))]
        (let [created-task (with-transaction [tx db/datasource]
                             (task/create tx
                                          {:tasks/name "Foo"
                                           :tasks/description "Description of Foo"}
                                          organization))
              retrieved-task (with-transaction [tx db/datasource]
                               (task/find-by-id tx (:tasks/id created-task)))
              now (time/now)]
          (is (= #:tasks{:id (:tasks/id created-task)
                         :name "Foo"
                         :description "Description of Foo"
                         :organization-id (:organizations/id organization)
                         :created-at now
                         :updated-at now
                         :archived-at nil}
                 retrieved-task)))))
    (testing "It throws an error if name is not present"
      (with-transaction [tx db/datasource]
        (is (thrown? org.postgresql.util.PSQLException
                     (task/create tx
                                  {:tasks/name nil
                                   :tasks/description "Description of Foo"}
                                  organization)))))))

(deftest find-by-id-test
  (let [{user-id :users/id} (factories/create-user)
        organization (factories/create-organization user-id)]
    (testing "It returns the task if the id is present in the DB"
      (let [task (factories/create-task organization)]
        (with-transaction [tx db/datasource]
          (is (= task
                 (task/find-by-id tx (:tasks/id task)))))))
    (testing "It returns nil if the id is not present in the DB"
      (with-transaction [tx db/datasource]
        (is (= nil
               (task/find-by-id tx Long/MAX_VALUE)))))))

(deftest list-test
  (let [{user-id :users/id} (factories/create-user)
        organization (factories/create-organization user-id)
        other-organization (factories/create-organization user-id)
        task1 (factories/create-task organization)
        task2 (factories/create-task organization)
        _task3 (factories/create-task other-organization)]
    (testing "It returns a list of tasks that match the given attributes"
      (with-transaction [tx db/datasource]
        (is (= #{task1 task2}
               (set (task/list tx {:organization-id (:organizations/id organization)}))))))
    (testing "It only returns un-archived tasks"
      (with-transaction [tx db/datasource]
        (db-task/update! tx {:id (:tasks/id task1)} {:archived-at (time/now)}))
      (with-transaction [tx db/datasource]
        (is (= #{task2}
               (set (task/list tx {:organization-id (:organizations/id organization)}))))))
    (testing "It returns an empty list if no tasks match the attributes"
      (let [attributes {:organization-id (->> :organizations/id
                                              s/gen
                                              (gen/such-that #(not (#{(:organizations/id organization)
                                                                      (:organizations/id other-organization)}
                                                                    %)))
                                              gen/generate)}]
        (with-transaction [tx db/datasource]
          (is (empty? (task/list tx attributes))))))))

(deftest update-test
  (let [{user-id :users/id} (factories/create-user)
        organization (factories/create-organization user-id)
        {:tasks/keys [id] :as task} (factories/create-task organization)
        now (time/now)]

    (testing "It updates the name of a task"
      (let [updated-name "Updated name"]
        (with-transaction [tx db/datasource]
          (task/update tx task {:name updated-name}))
        (with-transaction [tx db/datasource]
          (is (= updated-name
                 (:tasks/name (task/find-by-id tx id)))))))

    (testing "It updates the description of the task"
      (let [updated-description "Updated Description"]
        (with-transaction [tx db/datasource]
          (task/update tx task {:description updated-description}))
        (with-transaction [tx db/datasource]
          (is (= updated-description
                 (:tasks/description (task/find-by-id tx id)))))))

    (testing "It updates the updated-at time of the task"
      (with-redefs [time/now (constantly now)]
        (with-transaction [tx db/datasource]
          (task/update tx task {:name "New name" :description "New Description"}))
        (with-transaction [tx db/datasource]
          (let [{:tasks/keys [created-at updated-at]} (task/find-by-id tx id)]
            (is (= now updated-at))
            (is (pos? (compare updated-at created-at)))))))

    (testing "It does not update the archived-at time of the task"
      ;; Redefining the task since an update has already happened
      (let [{:tasks/keys [id] :as task} (factories/create-task organization)]
        (with-redefs [time/now (constantly now)]
          (with-transaction [tx db/datasource]
            (task/update tx task {:archived-at now}))
          (with-transaction [tx db/datasource]
            (let [{:tasks/keys [archived-at created-at updated-at]} (task/find-by-id tx id)]
              (is (not= now archived-at))
              (is (= created-at updated-at)))))))

    (testing "It throws an error if name is set to nil"
      (with-transaction [tx db/datasource]
        (is (thrown? org.postgresql.util.PSQLException
                     (task/update tx task {:name nil})))))))

(deftest archive-test
  (let [{user-id :users/id} (factories/create-user)
        organization (factories/create-organization user-id)
        {:tasks/keys [id] :as task} (factories/create-task organization)
        now (time/now)]
    (testing "It sets the archived-at to the current time"
      (with-redefs [time/now (constantly now)]
        (with-transaction [tx db/datasource]
          (task/archive tx task))
        (with-transaction [tx db/datasource]
          (let [{:tasks/keys [archived-at created-at updated-at]} (task/find-by-id tx id)]
            (is (= now archived-at))
            (is (= archived-at updated-at))
            (is (pos? (compare updated-at created-at)))))))))
