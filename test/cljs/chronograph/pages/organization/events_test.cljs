(ns chronograph.pages.organization.events-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests use-fixtures]]
            [day8.re-frame.test :as rf-test]
            [re-frame.core :as rf]
            [re-frame.db]
            [chronograph-web.pages.organization.events :as org-events]
            [chronograph-web.pages.organization.subscriptions :as org-subs]
            [chronograph-web.subscriptions :as subs]
            [chronograph.test-utils :as tu]
            [chronograph.fixtures :as fixtures]
            [chronograph-web.routes :as routes]))

(use-fixtures :once fixtures/check-specs)

(deftest fetch-organization-test
  (testing "When the API call for fetch organization succeeds"
    (rf-test/run-test-sync
     (tu/initialize-db!)
     (let [slug         "a-test-org"
           organization {:id         42
                         :name       "A Test Org"
                         :slug       slug
                         :created-at "2020-09-14T14:16:06.402873Z"
                         :updated-at "2020-09-14T14:16:06.402873Z"}]
       (rf/reg-fx :http-xhrio
         (fn [_]
           (rf/dispatch [::org-events/fetch-organization-success
                         organization])))
       (tu/set-token (routes/path-for :organization-show :slug slug))
       (rf/dispatch [::org-events/fetch-organization slug])
       (is (= {:id         42
               :name       "A Test Org"
               :slug       "a-test-org"
               :created-at "2020-09-14T14:16:06.402873Z"
               :updated-at "2020-09-14T14:16:06.402873Z"}
              @(rf/subscribe [::subs/organization slug]))
           "The fetched organization should be in the DB"))))

  (testing "When the API call for fetch organization fails"
    (rf-test/run-test-sync
     (tu/initialize-db!)
     (let [slug "a-test-org"
           error-params (tu/stub-effect :flash-error)]
       (rf/reg-fx :http-xhrio
         (fn [_]
           (rf/dispatch [::org-events/fetch-organization-fail])))
       (tu/set-token (routes/path-for :organization-show :slug slug))
       (rf/dispatch [::org-events/fetch-organization slug])
       (is (some? @error-params)
           "An error message should be flashed.")))))

(deftest fetch-members-test
  (testing "When fetch members API calls succeed"
    (rf-test/run-test-sync
     (tu/initialize-db!)
     (let [slug         "a-test-org"
           organization {:id         42
                         :name       "A Test Org"
                         :slug       slug
                         :created-at "2020-09-14T14:16:06.402873Z"
                         :updated-at "2020-09-14T14:16:06.402873Z"}]
       (tu/set-token (routes/path-for :organization-show :slug slug))
       (rf/dispatch [::org-events/fetch-organization-success
                     organization])
       (rf/reg-fx :http-xhrio
         (fn [_]
           (rf/dispatch [::org-events/fetch-members-succeeded
                         {:invited [{:id              1
                                     :organization-id 42
                                     :email           "foo@bar.com"}]
                          :joined  [{:id    1
                                     :name  "Sandy"
                                     :email "sandy@nilenso.com"}]}])))
       (rf/dispatch [::org-events/fetch-members slug])
       (is (= #{{:id              1
                 :organization-id 42
                 :email           "foo@bar.com"}}
              @(rf/subscribe [::org-subs/invited-members]))
           "The invited members should be in the DB")
       (is (= #{{:id    1
                 :name  "Sandy"
                 :email "sandy@nilenso.com"}}
              @(rf/subscribe [::org-subs/joined-members]))
           "The joined members should be in the DB"))))

  (testing "When fetching org members fails"
    (rf-test/run-test-sync
     (tu/initialize-db!)
     (rf/reg-fx :http-xhrio
       (fn [_]
         (rf/dispatch [::org-events/fetch-members-failed])))
     (let [error-params (tu/stub-effect :flash-error)]
       (rf/dispatch [::org-events/fetch-members ""])
       (is (some? @error-params)
           "An error message should be flashed.")))))

(deftest invite-member-form-test
  (testing "When the invite member form succeeds"
    (rf-test/run-test-sync
     (tu/initialize-db!)
     (tu/set-token (routes/path-for :organization-show :slug "test-slug"))
     (rf/dispatch [::org-events/fetch-organization-success
                   {:id   1
                    :name "A Test Org"
                    :slug "test-slug"}])
     (rf/dispatch [::org-events/invite-member-succeeded
                   {:id              1
                    :organization-id 1
                    :email           "foo@bar.com"}])
     (is (= #{{:id              1
               :organization-id 1
               :email           "foo@bar.com"}}
            @(rf/subscribe [::org-subs/invited-members]))
         "The invited member should be in the DB")))

  (testing "When the invite member form fails because the user is already in the organization"
    (rf-test/run-test-sync
     (tu/initialize-db!)
     (let [error-params (tu/stub-effect :flash-error)]
       (rf/dispatch [::org-events/invite-member-failed {:status 409}])
       (is (some? @error-params)
           "An error message should be flashed."))))

  (testing "When the invite member form fails for an unknown reason"
    (rf-test/run-test-sync
     (tu/initialize-db!)
     (let [error-params (tu/stub-effect :flash-error)]
       (rf/dispatch [::org-events/invite-member-failed])
       (is (some? @error-params)
           "An error message should be flashed.")))))

(deftest fetch-tasks-test
  (testing "When fetch tasks succeeds"
    (rf-test/run-test-sync
     (tu/initialize-db!)
     (tu/set-token (routes/path-for :organization-show :slug "test-slug"))
     (rf/dispatch [::org-events/fetch-organization-success
                   {:id   1
                    :name "A Test Org"
                    :slug "test-slug"}])
     (rf/reg-fx :http-xhrio
       (fn [_]
         (rf/dispatch [::org-events/fetch-tasks-success
                       [{:id              1
                         :name            "A task",
                         :description     "A Description"
                         :organization-id 1
                         :archived-at     nil}
                        {:id              2
                         :name            "Another task",
                         :description     "Another Description"
                         :organization-id 1
                         :archived-at     nil}]])))
     (rf/dispatch [::org-events/fetch-tasks])
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
            @(rf/subscribe [::org-subs/tasks]))
         "The task(s) are fetched and added to the db for the organization.")))

  (testing "When fetch task fails"
    (rf-test/run-test-sync
     (tu/initialize-db!)
     (tu/set-token (routes/path-for :organization-show :slug "test-slug"))
     (rf/reg-fx :http-xhrio
       (fn [_]
         (rf/dispatch [::org-events/fetch-tasks-failure])))

     (let [error-params (tu/stub-effect :flash-error)]
       (rf/dispatch [::org-events/fetch-tasks])
       (is (empty? @(rf/subscribe [::org-subs/tasks]))
           "There are no tasks for the organization in the db.")
       (is (some? @error-params)
           "An error message should be flashed.")))))

(deftest create-task-form-test
  (testing "When create task fails"
    (rf-test/run-test-sync
     (tu/initialize-db!)
     (let [error-params (tu/stub-effect :flash-error)]
       (rf/dispatch [::org-events/create-task-failed])
       (is (empty? @(rf/subscribe [::org-subs/tasks]))
           "There are no tasks for the organization in the db.")
       (is (some? @error-params)
           "An error message should be flashed.")))))

(deftest update-task-form-test
  (testing "When updating a task succeeds"
    (rf-test/run-test-sync
     (tu/initialize-db!)
     (tu/set-token (routes/path-for :organization-show :slug "test-slug"))
     (rf/dispatch [::org-events/fetch-organization-success
                   {:id   1
                    :name "A Test Org"
                    :slug "test-slug"}])
     (rf/dispatch [::org-events/fetch-tasks-success
                   [{:id              1
                     :name            "A task",
                     :description     "A Description"
                     :organization-id 1
                     :archived-at     nil}
                    {:id              2
                     :name            "Another task",
                     :description     "Another Description"
                     :organization-id 1
                     :archived-at     nil}]])
     (let [updated-tasks [{:id              1
                           :name            "A task",
                           :description     "A Description"
                           :organization-id 1
                           :archived-at     nil}
                          {:id              2
                           :name            "Updated name",
                           :description     "Updated description"
                           :organization-id 1
                           :archived-at     nil}]]
       (rf/reg-fx :http-xhrio
         (fn [& _]
           (rf/dispatch [::org-events/fetch-tasks-success
                         updated-tasks])))

       (rf/dispatch [::org-events/update-task-success 2])

       (is (= updated-tasks
              @(rf/subscribe [::org-subs/tasks]))
           "The tasks in the DB should be updated"))))

  (testing "When updating a task fails"
    (rf-test/run-test-sync
     (tu/initialize-db!)
     (tu/set-token (routes/path-for :organization-show :slug "test-slug"))
     (let [error-params (tu/stub-effect :flash-error)]
       (rf/dispatch [::org-events/update-task-failure 2])
       (is (some? @error-params)
           "An error message should be flashed.")))))

(deftest show-and-hide-update-task-form-test
  (testing "When the update form is shown or hidden for a task, the flag should be set accordingly"
    (rf-test/run-test-sync
     (tu/initialize-db!)
     (tu/set-token (routes/path-for :organization-show :slug "test-slug"))
     (rf/dispatch [::org-events/fetch-organization-success
                   {:id   1
                    :name "A Test Org"
                    :slug "test-slug"}])
     (let [task1 {:id              1
                  :name            "A task",
                  :description     "A Description"
                  :organization-id 1
                  :archived-at     nil}
           task2 {:id              2
                  :name            "Another task",
                  :description     "Another Description"
                  :organization-id 1
                  :archived-at     nil}]
       (rf/dispatch [::org-events/fetch-tasks-success [task1 task2]])

       (rf/dispatch [::org-events/show-update-task-form 2])
       (is (not @(rf/subscribe [::org-subs/show-update-task-form? 1])))
       (is @(rf/subscribe [::org-subs/show-update-task-form? 2]))

       (rf/dispatch [::org-events/hide-update-task-form 2])
       (is (not @(rf/subscribe [::org-subs/show-update-task-form? 1])))
       (is (not @(rf/subscribe [::org-subs/show-update-task-form? 2])))))))
