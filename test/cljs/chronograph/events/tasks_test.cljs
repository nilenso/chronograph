(ns chronograph.events.tasks-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests use-fixtures]]
            [chronograph-web.events.tasks :as tasks-events]
            [day8.re-frame.test :as rf-test]
            [chronograph.test-utils :as tu]
            [chronograph-web.routes :as routes]
            [re-frame.core :as rf]
            [re-frame.db]
            [chronograph-web.db.tasks :as tasks-db]
            [chronograph-web.db.organization :as org-db]
            [chronograph.fixtures :as fixtures]))

(use-fixtures :once fixtures/silence-logging fixtures/check-specs)

(deftest fetch-tasks-test
  (testing "When fetch tasks succeeds"
    (rf-test/run-test-sync
     (tu/initialize-db!)
     (tu/set-token (routes/path-for :organization-show :slug "test-slug"))
     (swap! re-frame.db/app-db org-db/add-org {:id   1
                                               :name "A Test Org"
                                               :slug "test-slug"
                                               :role "member"})
     (tu/stub-xhrio [{:id              1
                      :name            "A task",
                      :description     "A Description"
                      :organization-id 1
                      :archived-at     nil}
                     {:id              2
                      :name            "Another task",
                      :description     "Another Description"
                      :organization-id 1
                      :archived-at     nil}]
                    true)
     (rf/dispatch [::tasks-events/fetch-tasks "test-slug"])
     (is (= [{:id              1
              :name            "A task",
              :description     "A Description"
              :organization-id 1
              :archived-at     nil}
             {:id              2
              :name            "Another task",
              :description     "Another Description"
              :organization-id 1
              :archived-at     nil}]
            (tasks-db/current-organization-tasks @re-frame.db/app-db))
         "The task(s) are fetched and added to the db for the organization.")))

  (testing "When fetch task fails"
    (rf-test/run-test-sync
     (tu/initialize-db!)
     (tu/set-token (routes/path-for :organization-show :slug "test-slug"))
     (rf/reg-fx :http-xhrio
       (fn [_]
         (rf/dispatch [::tasks-events/fetch-tasks-failure])))

     (let [error-params (tu/stub-effect :flash-error)]
       (rf/dispatch [::tasks-events/fetch-tasks "test-slug"])
       (is (empty? (tasks-db/current-organization-tasks @re-frame.db/app-db))
           "There are no tasks for the organization in the db.")
       (is (some? @error-params)
           "An error message should be flashed.")))))
