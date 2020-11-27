(ns chronograph-web.pages.admin.events-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests use-fixtures]]
            [re-frame.core :as rf]
            [re-frame.db]
            [chronograph-web.pages.admin.events :as org-events]
            [chronograph-web.events.tasks :as task-events]
            [chronograph-web.pages.admin.subscriptions :as org-subs]
            [chronograph-web.subscriptions :as subs]
            [chronograph-web.test-utils :as tu]
            [chronograph-web.fixtures :as fixtures]
            [chronograph-web.routes :as routes]
            [chronograph-web.db.organization :as org-db]
            [chronograph-web.db.tasks :as db-tasks]))

(use-fixtures :once fixtures/silence-logging fixtures/check-specs)

(deftest organization-page-navigated-test
  (tu/rf-test "When the org page is navigated to, the organization and tasks should be fetched"
    (let [fetch-org-event   (tu/stub-event ::org-events/fetch-organization)
          fetch-tasks-event (tu/stub-event ::task-events/fetch-tasks)]
      (tu/set-token (routes/path-for :admin-page :slug "test-slug"))
      (rf/dispatch [::org-events/organization-page-navigated])
      (is (= :admin-page @(rf/subscribe [::subs/page-key])) "the page-key should be set")
      (is (= [::org-events/fetch-organization "test-slug"]
             @fetch-org-event))
      (is (= [::task-events/fetch-tasks "test-slug"]
             @fetch-tasks-event)))))

(deftest fetch-organization-test
  (testing "When the API call for fetch organization succeeds"
    (tu/rf-test "When the user is a member of the organization"
      (let [slug             "a-test-org"
            organization     {:id         42
                              :name       "A Test Org"
                              :slug       slug
                              :role       "member"
                              :created-at "2020-09-14T14:16:06.402873Z"
                              :updated-at "2020-09-14T14:16:06.402873Z"}
            dispatched-event (tu/stub-event ::org-events/fetch-members)]
        (rf/reg-fx :http-xhrio
          (fn [_]
            (rf/dispatch [::org-events/fetch-organization-success
                          organization])))
        (tu/set-token (routes/path-for :admin-page :slug slug))
        (rf/dispatch [::org-events/fetch-organization slug])
        (is (= {:id         42
                :name       "A Test Org"
                :slug       "a-test-org"
                :role       "member"
                :created-at "2020-09-14T14:16:06.402873Z"
                :updated-at "2020-09-14T14:16:06.402873Z"}
               @(rf/subscribe [::subs/organization slug]))
            "The fetched organization should be in the DB")
        (is (= nil
               @dispatched-event)
            "The members should not be fetched")))

    (tu/rf-test "When the user is an admin of the organization"
      (let [slug             "a-test-org"
            organization     {:id   42
                              :name "A Test Org"
                              :slug slug
                              :role "admin"}
            dispatched-event (tu/stub-event ::org-events/fetch-members)]
        (rf/reg-fx :http-xhrio
          (fn [_]
            (rf/dispatch [::org-events/fetch-organization-success
                          organization])))
        (tu/set-token (routes/path-for :admin-page :slug slug))
        (rf/dispatch [::org-events/fetch-organization slug])
        (is (= {:id   42
                :name "A Test Org"
                :slug slug
                :role "admin"}
               @(rf/subscribe [::subs/organization slug]))
            "The fetched organization should be in the DB")
        (is (= [::org-events/fetch-members slug]
               @dispatched-event)
            "The members should be fetched"))))

  (tu/rf-test "When the API call for fetch organization fails"
    (let [slug         "a-test-org"
          error-params (tu/stub-effect :flash-error)]
      (rf/reg-fx :http-xhrio
        (fn [_]
          (rf/dispatch [::org-events/fetch-organization-fail])))
      (tu/set-token (routes/path-for :admin-page :slug slug))
      (rf/dispatch [::org-events/fetch-organization slug])
      (is (some? @error-params)
          "An error message should be flashed."))))

(deftest fetch-members-test
  (tu/rf-test "When fetch members API calls succeed"
    (let [slug         "a-test-org"
          organization {:id         42
                        :name       "A Test Org"
                        :slug       slug
                        :role       "member"
                        :created-at "2020-09-14T14:16:06.402873Z"
                        :updated-at "2020-09-14T14:16:06.402873Z"}]
      (tu/set-token (routes/path-for :admin-page :slug slug))
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
          "The joined members should be in the DB")))

  (tu/rf-test "When fetching org members fails"
    (rf/reg-fx :http-xhrio
      (fn [_]
        (rf/dispatch [::org-events/fetch-members-failed])))
    (let [error-params (tu/stub-effect :flash-error)]
      (rf/dispatch [::org-events/fetch-members ""])
      (is (some? @error-params)
          "An error message should be flashed."))))

(deftest invite-member-form-test
  (tu/rf-test "When the invite member form succeeds"
    (tu/set-token (routes/path-for :admin-page :slug "test-slug"))
    (rf/dispatch [::org-events/fetch-organization-success
                  {:id   1
                   :name "A Test Org"
                   :slug "test-slug"
                   :role "member"}])
    (rf/dispatch [::org-events/invite-member-succeeded
                  {:id              1
                   :organization-id 1
                   :email           "foo@bar.com"}])
    (is (= #{{:id              1
              :organization-id 1
              :email           "foo@bar.com"}}
           @(rf/subscribe [::org-subs/invited-members]))
        "The invited member should be in the DB"))

  (tu/rf-test "When the invite member form fails because the user is already in the organization"
    (let [error-params (tu/stub-effect :flash-error)]
      (rf/dispatch [::org-events/invite-member-failed {:status 409}])
      (is (some? @error-params)
          "An error message should be flashed.")))

  (tu/rf-test "When the invite member form fails for an unknown reason"
    (let [error-params (tu/stub-effect :flash-error)]
      (rf/dispatch [::org-events/invite-member-failed])
      (is (some? @error-params)
          "An error message should be flashed."))))

(deftest create-task-form-test
  (tu/rf-test "When create task fails"
    (let [error-params (tu/stub-effect :flash-error)]
      (rf/dispatch [::org-events/create-task-failed])
      (is (empty? @(rf/subscribe [::org-subs/tasks]))
          "There are no tasks for the organization in the db.")
      (is (some? @error-params)
          "An error message should be flashed."))))

(deftest update-task-form-test
  (tu/rf-test "When updating a task succeeds"
    (tu/set-token (routes/path-for :admin-page :slug "test-slug"))
    (swap! re-frame.db/app-db org-db/add-org {:id   1
                                              :name "A Test Org"
                                              :slug "test-slug"
                                              :role "admin"})
    (swap! re-frame.db/app-db db-tasks/merge-tasks [{:id              1
                                                     :name            "A task",
                                                     :description     "A Description"
                                                     :organization-id 1
                                                     :archived-at     nil}
                                                    {:id              2
                                                     :name            "Another task",
                                                     :description     "Another Description"
                                                     :organization-id 1
                                                     :archived-at     nil}])
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
          (rf/dispatch [::task-events/fetch-tasks-success
                        updated-tasks])))

      (rf/dispatch [::org-events/update-task-success 2])

      (is (= updated-tasks
             @(rf/subscribe [::org-subs/tasks]))
          "The tasks in the DB should be updated")
      (is (not @(rf/subscribe [::org-subs/show-update-task-form? 2]))
          "The task form should be closed")))

  (tu/rf-test "When updating a task fails"
    (tu/set-token (routes/path-for :admin-page :slug "test-slug"))
    (let [error-params (tu/stub-effect :flash-error)]
      (rf/dispatch [::org-events/update-task-failure 2])
      (is (some? @error-params)
          "An error message should be flashed."))))

(deftest show-and-hide-update-task-form-test
  (tu/rf-test "When the update form is shown or hidden for a task, the flag should be set accordingly"
    (tu/set-token (routes/path-for :admin-page :slug "test-slug"))
    (rf/dispatch [::org-events/fetch-organization-success
                  {:id   1
                   :name "A Test Org"
                   :slug "test-slug"
                   :role "member"}])
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
      (swap! re-frame.db/app-db db-tasks/merge-tasks [task1 task2])

      (rf/dispatch [::org-events/show-update-task-form 2])
      (is (not @(rf/subscribe [::org-subs/show-update-task-form? 1])))
      (is @(rf/subscribe [::org-subs/show-update-task-form? 2]))

      (rf/dispatch [::org-events/hide-update-task-form 2])
      (is (not @(rf/subscribe [::org-subs/show-update-task-form? 1])))
      (is (not @(rf/subscribe [::org-subs/show-update-task-form? 2]))))))
