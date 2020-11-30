(ns chronograph.handlers.timer-test
  (:require [chronograph.handlers.timer :as handler-timer]
            [clojure.test :refer :all]
            [chronograph.factories :as factories]
            [chronograph.domain.acl :as acl]
            [chronograph.db.core :as db]
            [clojure.spec.alpha :as s]
            [chronograph.domain.timer :as timer]
            [chronograph.fixtures :as fixtures]
            [chronograph.utils.time :as time]
            [chronograph.test-utils :as tu])
  (:import [java.time LocalDate Instant]
           (java.util UUID)))

(defn- setup-org-users-tasks!
  []
  (let [{user-1-id :users/id}
        (factories/create-user)

        {organization-id :organizations/id
         :as organization}
        (factories/create-organization user-1-id)

        {user-2-id :users/id} (factories/create-user)
        _ (acl/create! db/datasource
                       #:acls{:user-id user-2-id
                              :organization-id organization-id
                              :role acl/member})
        {task-id-1 :tasks/id :as task-1} (factories/create-task organization)
        {task-id-2 :tasks/id :as task-2} (factories/create-task organization)]
    {:user-1-id user-1-id
     :organization organization
     :organization-id organization-id
     :user-2-id user-2-id
     :task-id-1 task-id-1
     :task-id-2 task-id-2
     :task-1 task-1
     :task-2 task-2}))

(use-fixtures :once fixtures/config fixtures/datasource)
(use-fixtures :each fixtures/clear-db)

(def default-recorded-for "2020-01-14")

(deftest create-timer-test
  (let [test-context (setup-org-users-tasks!)]
    (testing "when a timer is created without a note"
      (let [response (handler-timer/create {:body {:task-id (:task-id-1 test-context)
                                                   :recorded-for default-recorded-for}
                                            :user {:users/id (:user-2-id test-context)}})]
        (is (= 200 (:status response))
            "the request succeeds")
        (is (s/valid? :timers/timer
                      (:body response))
            "the body is a valid timer object")
        (is (timer/running? (:body response))
            "the timer should be running")
        (is (= ""
               (:timers/note (:body response)))
            "the note is an empty string.")))

    (testing "when a timer is created with a note"
      (let [note "A sample note."
            response (handler-timer/create {:body {:task-id (:task-id-1 test-context)
                                                   :note note
                                                   :recorded-for default-recorded-for}
                                            :user {:users/id (:user-2-id test-context)}})]
        (is (= 200 (:status response))
            "the request succeeds")
        (is (s/valid? :timers/timer
                      (:body response))
            "the body is a valid timer object")
        (is (timer/running? (:body response))
            "the timer should be running")
        (is (= note
               (:timers/note (:body response)))
            "the timer contains the note we added")))

    (testing "when a timer is created with an invalid note and/or invalid task id"
      (is (= {:status 400
              :headers {}
              :body {:error "Invalid Task ID or Note."}}
             (handler-timer/create {:body {:task-id "foo"
                                           :note 42
                                           :recorded-for default-recorded-for}
                                    :user {:users/id (:user-2-id test-context)}}))
          "the request fails with an HTTP error."))

    (testing "when a timer is created with a non-existent task id"
      (is (= {:status 400
              :headers {}
              :body {:error "Task does not exist."}}
             (handler-timer/create {:body {:task-id Long/MAX_VALUE
                                           :note "A valid note."
                                           :recorded-for default-recorded-for}
                                    :user {:users/id (:user-2-id test-context)}}))
          "the request fails with an HTTP error."))

    (testing "when a user does not belong to the organization in the request"
      (let [org (factories/create-user-and-organization)
            {task-id :tasks/id} (factories/create-task org)]
        (is (= {:status  403
                :headers {}
                :body    {:error "Forbidden."}}
               (handler-timer/create {:body         {:task-id      task-id
                                                     :note         "A valid note."
                                                     :recorded-for default-recorded-for}
                                      :user         {:users/id (:user-2-id test-context)}}))
            "the request fails with an HTTP error.")))))

(deftest delete-timer-test
  (testing "deleting a timer"
    (let [test-context (setup-org-users-tasks!)
          {timer-id :timers/id :as timer} (factories/create-timer
                                           (:organization-id test-context)
                                           (:user-2-id test-context)
                                           (:task-id-1 test-context))
          response (handler-timer/delete {:params {:timer-id (str timer-id)}
                                          :user {:users/id (:user-2-id test-context)}})]
      (is (= 200
             (:status response))
          "the request succeeds")
      (is (= timer
             (:body response))
          "the body contains the timer ID that was just deleted")
      (is (= {:status 400,
              :headers {},
              :body {:error "Timer does not exist."}}
             (handler-timer/delete {:params {:timer-id (str timer-id)}
                                    :user {:users/id (:user-2-id test-context)}}))
          "fails with HTTP error when the timer is already deleted.")
      (is (= {:status 400
              :headers {}
              :body {:error "Invalid Timer ID."}}
             (handler-timer/delete {:params {:timer-id "42"}
                                    :user {:users/id (:user-2-id test-context)}}))
          "fails with HTTP error when the timer id is invalid"))))

(deftest update-test
  (testing "Updating with valid parameters"
    (tu/with-fixtures [fixtures/clear-db]
      (with-redefs [time/now (constantly (Instant/parse "2020-11-30T11:30:00Z"))]
        (let [test-context (setup-org-users-tasks!)
              {timer-id :timers/id} (factories/create-timer
                                     (:organization-id test-context)
                                     (:user-2-id test-context)
                                     (:task-id-1 test-context))
              {:keys [status body]} (handler-timer/update {:params {:timer-id (str timer-id)}
                                                           :body   {:duration-in-secs 1800
                                                                    :note             "Updated note"
                                                                    :task-id          (:task-id-1 test-context)}
                                                           :user   {:users/id (:user-2-id test-context)}})]
          (is (= 200 status))
          (is (s/valid? :timers/timer body))))))

  (testing "Updating with a task ID from a different organization"
    (tu/with-fixtures [fixtures/clear-db]
      (with-redefs [time/now (constantly (Instant/parse "2020-11-30T11:30:00Z"))]
        (let [test-context (setup-org-users-tasks!)
              {timer-id :timers/id} (factories/create-timer
                                     (:organization-id test-context)
                                     (:user-2-id test-context)
                                     (:task-id-1 test-context))
              organization (factories/create-organization (:user-2-id test-context))
              {task-id-3 :tasks/id} (factories/create-task organization)
              response (handler-timer/update {:params {:timer-id (str timer-id)}
                                              :body   {:duration-in-secs 1800
                                                       :note             "Updated note"
                                                       :task-id          task-id-3}
                                              :user   {:users/id (:user-2-id test-context)}})]
          (is (= {:status 400
                  :body {:error "Task not found."}}
                 (select-keys response [:status :body])))))))

  (testing "Updating a non-existent timer"
    (tu/with-fixtures [fixtures/clear-db]
      (with-redefs [time/now (constantly (Instant/parse "2020-11-30T11:30:00Z"))]
        (let [test-context (setup-org-users-tasks!)
              response     (handler-timer/update {:params {:timer-id (str (UUID/randomUUID))}
                                                  :body   {:duration-in-secs 1800
                                                           :note             "Updated note"
                                                           :task-id          (:task-id-1 test-context)}
                                                  :user   {:users/id (:user-2-id test-context)}})]
          (is (= {:status 400
                  :body {:error "Timer not found."}}
                 (select-keys response [:status :body]))))))))

(deftest start-timer-test
  (testing "starting a timer"
    (let [test-context (setup-org-users-tasks!)
          {timer-id :timers/id} (factories/create-timer
                                 (:organization-id test-context)
                                 (:user-2-id test-context)
                                 (:task-id-1 test-context))
          response (handler-timer/start {:params {:timer-id (str timer-id)}
                                         :user {:users/id (:user-2-id test-context)}})]
      (is (= 200
             (:status response))
          "the request succeeds")
      (is (s/valid? :timers/timer
                    (:body response))
          "the body is the timer that was just started")
      (is (= {:status 400,
              :headers {},
              :body {:error "Timer is already started."}}
             (handler-timer/start {:params {:timer-id (str timer-id)}
                                   :user {:users/id (:user-2-id test-context)}}))
          "fails with HTTP error when the timer is already started.")
      (is (= {:status 400
              :headers {}
              :body {:error "Timer does not exist."}}
             (handler-timer/start {:params {:timer-id (str (java.util.UUID/randomUUID))}
                                   :user {:users/id (:user-2-id test-context)}}))
          "fails with HTTP error when the timer does not exist")
      (is (= {:status 400
              :headers {}
              :body {:error "Invalid Timer ID."}}
             (handler-timer/start {:params {:timer-id "42"}
                                   :user {:users/id (:user-2-id test-context)}}))
          "fails with HTTP error when the timer id is invalid"))))

(deftest stop-timer-test
  (testing "stopping a timer"
    (let [test-context (setup-org-users-tasks!)
          user-2-id (:user-2-id test-context)
          {timer-id :timers/id} (factories/create-timer
                                 (:organization-id test-context)
                                 (:user-2-id test-context)
                                 (:task-id-1 test-context))
          _ (timer/start! db/datasource user-2-id timer-id)
          response (handler-timer/stop {:params {:timer-id (str timer-id)}
                                        :user {:users/id user-2-id}})]
      (is (= 200
             (:status response))
          "the request succeeds")
      (is (s/valid? :timers/timer
                    (:body response))
          "the body is the timer that was just stopped")
      (is (= {:status 400,
              :headers {},
              :body {:error "Timer is already stopped."}}
             (handler-timer/stop {:params {:timer-id (str timer-id)}
                                  :user {:users/id (:user-2-id test-context)}}))
          "fails with HTTP error when the timer is already stopped.")
      (is (= {:status 400
              :headers {}
              :body {:error "Timer does not exist."}}
             (handler-timer/stop {:params {:timer-id (str (java.util.UUID/randomUUID))}}))
          "fails with HTTP error when the timer does not exist")
      (is (= {:status 400
              :headers {}
              :body {:error "Invalid Timer ID."}}
             (handler-timer/stop {:params {:timer-id "42"}}))
          "fails with HTTP error when the timer id is invalid"))))

(deftest find-timers-for-user-and-task-test
  (testing "when a timer is looked up by user id"
    (let [test-context (setup-org-users-tasks!)
          user-2-id (:user-2-id test-context)
          task-id (:task-id-1 test-context)
          _timers (dotimes [_ 3]
                    (->> (factories/create-timer (:organization-id test-context)
                                                 #:timers{:user-id user-2-id
                                                          :task-id task-id
                                                          :note "A lovely timer."})
                         :timers/id
                         (timer/start! db/datasource user-2-id)))
          response (handler-timer/find-by-user-and-task {:params {:task-id task-id}
                                                         :user   {:users/id user-2-id}})]
      (is (= 200
             (:status response))
          "the request succeeds for an existing timer")
      (is (s/valid? (s/coll-of :timers/timer)
                    (:body response))
          "the body of is a collection of timer objects")
      (is (= {:status 404
              :headers {}
              :body {:error "Timers not found."}}
             (handler-timer/find-by-user-and-task {:params {:task-id Long/MAX_VALUE}
                                                   :user   {:users/id user-2-id}}))
          "fails with HTTP error when timers do not exist"))))

(deftest find-by-day-test
  (testing "when timers exist for a given day"
    (let [{:keys [organization-id user-2-id task-id-1 task-1]} (setup-org-users-tasks!)
          timer-1 (factories/create-timer organization-id
                                          #:timers{:user-id user-2-id
                                                   :task-id task-id-1
                                                   :recorded-for (LocalDate/parse "2020-01-13")
                                                   :note "A lovely timer."})
          timer-2 (factories/create-timer organization-id
                                          #:timers{:user-id user-2-id
                                                   :task-id task-id-1
                                                   :recorded-for (LocalDate/parse "2020-01-13")
                                                   :note "A lovely timer."})
          response (handler-timer/find-by-day {:params {:day "2020-01-13"}
                                               :user {:users/id user-2-id}})]
      (is (= 200 (:status response)))
      (is (= {:timers [(assoc timer-1 :task task-1) (assoc timer-2 :task task-1)]}
             (:body response))))))
