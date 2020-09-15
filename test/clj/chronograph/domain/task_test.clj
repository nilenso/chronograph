(ns chronograph.domain.task-test
  (:require [clojure.test :refer :all]
            [chronograph.fixtures :as fixtures]
            [chronograph.domain.task :as task]
            [chronograph.utils.time :as time]))

(use-fixtures :once fixtures/config fixtures/datasource)
(use-fixtures :each fixtures/clear-db)

(deftest create-task-test
  (testing "Can create task with name and description"
    (with-redefs [time/now (constantly (time/now))]
      (let [created-task (task/create "Foo"
                                      "Description of Foo")
            retrieved-task (task/find-by-id (:tasks/id created-task))
            now (time/now)]
        (is (= #:tasks{:id (:tasks/id created-task)
                       :name "Foo"
                       :description "Description of Foo"
                       :created-at now
                       :updated-at now
                       :archived-at nil}
               retrieved-task))))))
