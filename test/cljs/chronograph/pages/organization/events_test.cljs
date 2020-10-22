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
            [chronograph-web.db :as db]
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
     (let [slug "a-test-org"]
       (rf/reg-fx :http-xhrio
         (fn [_]
           (rf/dispatch [::org-events/fetch-organization-fail])))
       (tu/set-token (routes/path-for :organization-show :slug slug))
       (rf/dispatch [::org-events/fetch-organization slug])
       (is (contains? @(rf/subscribe [::subs/page-errors]) ::org-events/error-org-not-found)
           "The reported error should be in the DB")))))

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
     (rf/dispatch [::org-events/fetch-members ""])
     (is (contains? @(rf/subscribe [::subs/page-errors])
                    ::org-events/error-fetch-members-failed)
         "The reported error should be in the DB"))))

(deftest invite-member-form-test
  (testing "When the invite member form succeeds"
    (rf-test/run-test-sync
     (tu/initialize-db!)
     (tu/set-token (routes/path-for :organization-show :slug "test-slug"))
     (rf/dispatch [::org-events/fetch-organization-success
                   {:id   1
                    :name "A Test Org"
                    :slug "test-slug"}])
     (swap! re-frame.db/app-db
            db/report-error
            ::error-invite-member-failed)
     (rf/dispatch [::org-events/invite-member-succeeded
                   {:id              1
                    :organization-id 1
                    :email           "foo@bar.com"}])
     (is (= #{{:id              1
               :organization-id 1
               :email           "foo@bar.com"}}
            @(rf/subscribe [::org-subs/invited-members]))
         "The invited member should be in the DB")
     (is (not (contains? @(rf/subscribe [::subs/page-errors])
                         ::org-events/error-invite-member-failed))
         "Error state if any is cleared from the db")))

  (testing "When the invite member form fails"
    (rf-test/run-test-sync
     (tu/initialize-db!)
     (rf/dispatch [::org-events/invite-member-failed])
     (is (contains? @(rf/subscribe [::subs/page-errors])
                    ::org-events/error-invite-member-failed)
         "The reported error should be in the DB"))))

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
     (rf/dispatch [::org-events/fetch-tasks])
     (is (empty? @(rf/subscribe [::org-subs/tasks]))
         "There are no tasks for the organization in the db.")
     (is (contains? @(rf/subscribe [::subs/page-errors])
                    ::org-events/error-fetch-tasks-failed)
         "The page error is added to the db."))))

(deftest create-task-form-test
  (testing "When create task fails"
    (rf-test/run-test-sync
     (tu/initialize-db!)
     (rf/dispatch [::org-events/create-task-failed])
     (is (empty? @(rf/subscribe [::org-subs/tasks]))
         "There are no tasks for the organization in the db.")
     (is (contains? @(rf/subscribe [::subs/page-errors])
                    ::org-events/error-creating-task-failed)
         "The page error is added to the db."))))

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
           "The tasks in the DB should be updated")
       (is (not (contains? @(rf/subscribe [::subs/page-errors])
                           ::error-update-task-failed))
           "The update error is removed, if any"))))

  (testing "When updating a task fails"
    (rf-test/run-test-sync
     (tu/initialize-db!)
     (tu/set-token (routes/path-for :organization-show :slug "test-slug"))

     (rf/dispatch [::org-events/update-task-failure 2])

     (is (contains? @(rf/subscribe [::subs/page-errors])
                    ::org-events/error-update-task-failed)
         "The update error should be reported"))))

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
       (is (= [task1
               (assoc task2 :is-updating true)]
              @(rf/subscribe [::org-subs/tasks])))
       (rf/dispatch [::org-events/hide-update-task-form 2])
       (is (= [task1
               (assoc task2 :is-updating false)]
              @(rf/subscribe [::org-subs/tasks])))))))
