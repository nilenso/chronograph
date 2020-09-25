(ns chronograph.handlers.timer-test
  (:require [chronograph.handlers.timer :as handler-timer]
            [clojure.test :refer :all]
            [chronograph.factories :as factories]
            [chronograph.domain.acl :as acl]
            [chronograph.db.core :as db]
            [clojure.spec.alpha :as s]
            [chronograph.domain.timer :as timer]
            [chronograph.fixtures :as fixtures]))

(def test-context
  (atom {}))

(defn- setup-members-org-tasks!
  []
  (let [{owner-id :users/id}
        (factories/create-user)

        {organization-id :organizations/id
         :as organization}
        (factories/create-organization owner-id)

        {member-id :users/id} (factories/create-user)
        _ (acl/create! db/datasource
                       {:user-id member-id
                        :organization-id organization-id
                        :role acl/member})
        {task-id-1 :tasks/id} (factories/create-task organization)
        {task-id-2 :tasks/id} (factories/create-task organization)]
    (reset! test-context
            {:owner-id owner-id
             :organization organization
             :organization-id organization-id
             :member-id member-id
             :task-id-1 task-id-1
             :task-id-2 task-id-2})))

(defn- setup-members-org-tasks-fixture
  [f]
  (setup-members-org-tasks!)
  (f))

(use-fixtures :once fixtures/config fixtures/datasource)
(use-fixtures :each
  fixtures/clear-db
  setup-members-org-tasks-fixture)

(deftest create-timer-test
  (testing "when a timer is created without a note"
    (let [response (handler-timer/create {:body {:task-id (:task-id-1 @test-context)}
                                          :user {:users/id (:member-id @test-context)}
                                          :organization (:organization @test-context)})]
      (is (= 200 (:status response))
          "the request succeeds")
      (is (s/valid? :timers/timer
                    (:body response))
          "the body is a valid timer object")
      (is (= ""
             (:timers/note (:body response)))
          "the note is an empty string.")))

  (testing "when a timer is created with a note"
    (let [note "A sample note."
          response (handler-timer/create {:body {:task-id (:task-id-1 @test-context)
                                                 :note note}
                                          :user {:users/id (:member-id @test-context)}
                                          :organization (:organization @test-context)})]
      (is (= 200 (:status response))
          "the request succeeds")
      (is (s/valid? :timers/timer
                    (:body response))
          "the body is a valid timer object")
      (is (= note
             (:timers/note (:body response)))
          "the timer contains the note we added")))

  (testing "when a timer is created with an invalid note and/or invalid task id"
    (is (= {:status 400
            :headers {}
            :body {:error "Invalid Task ID or Note."}}
           (handler-timer/create {:body {:task-id "foo"
                                         :note 42}
                                  :user {:users/id (:member-id @test-context)}
                                  :organization (:organization @test-context)}))
        "the request fails with an HTTP error."))

  (testing "when a timer is created with a non-existent task id"
    (is (= {:status 400
            :headers {}
            :body {:error "Task does not exist."}}
           (handler-timer/create {:body {:task-id Long/MAX_VALUE
                                         :note "A valid note."}
                                  :user {:users/id (:member-id @test-context)}
                                  :organization (:organization @test-context)}))
        "the request fails with an HTTP error."))

  (testing "when a user does not belong to the organization in the request"
    (is (= {:status 403
            :headers {}
            :body {:error "Forbidden."}}
           (handler-timer/create {:body {:task-id Long/MAX_VALUE
                                         :note "A valid note."}
                                  :user {:users/id (:member-id @test-context)}
                                  :organization (factories/create-organization
                                                 (:users/id
                                                  (factories/create-user)))}))
        "the request fails with an HTTP error.")))

(deftest delete-timer-test
  (testing "deleting a timer"
    (let [{timer-id :timers/id :as timer} (timer/create! db/datasource
                                                         (:member-id @test-context)
                                                         (:task-id-1 @test-context)
                                                         "A lovely timer.")
          response (handler-timer/delete {:params {:timer-id (str timer-id)}
                                          :user {:users/id (:member-id @test-context)}})]
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
                                    :user {:users/id (:member-id @test-context)}}))
          "fails with HTTP error when the timer is already deleted.")
      (is (= {:status 400
              :headers {}
              :body {:error "Invalid Timer ID."}}
             (handler-timer/delete {:params {:timer-id "42"}
                                    :user {:users/id (:member-id @test-context)}}))
          "fails with HTTP error when the timer id is invalid"))))

(deftest update-timer-note-test
  (testing "updating a timer's note"
    (let [{timer-id :timers/id} (timer/create! db/datasource
                                               (:member-id @test-context)
                                               (:task-id-1 @test-context)
                                               "A lovely timer.")
          response (handler-timer/update-note {:params {:timer-id (str timer-id)}
                                               :body {:note "An updated note."}
                                               :user {:users/id (:member-id @test-context)}})]
      (is (= 200
             (:status response))
          "the request succeeds")
      (is (s/valid? :timers/timer
                    (:body response))
          "the body is a valid timer object")
      (is (= "An updated note."
             (:timers/note (:body response)))
          "the timer data contains the updated note")
      (is (= {:status 400
              :headers {}
              :body {:error "Timer does not exist."}}
             (handler-timer/update-note {:params {:timer-id (str (java.util.UUID/randomUUID))}
                                         :body {:note "An updated note."}
                                         :user {:users/id (:member-id @test-context)}}))
          "fails with HTTP error when the timer does not exist")
      (is (= {:status 400
              :headers {}
              :body {:error "Invalid Timer ID or Note."}}
             (handler-timer/update-note {:params {:timer-id "42"}
                                         :body {:note nil}
                                         :user {:users/id (:member-id @test-context)}}))
          "fails with HTTP error when the timer id and/or note are invalid"))))

(deftest start-timer-test
  (testing "starting a timer"
    (let [{timer-id :timers/id} (timer/create! db/datasource
                                               (:member-id @test-context)
                                               (:task-id-1 @test-context)
                                               "A lovely timer.")
          response (handler-timer/start {:params {:timer-id (str timer-id)}
                                         :user {:users/id (:member-id @test-context)}})]
      (is (= 200
             (:status response))
          "the request succeeds")
      (is (s/valid? :time-spans/time-span
                    (:body response))
          "the body is a valid time span that was just started")
      (is (= {:status 400,
              :headers {},
              :body {:error "Timer is already started."}}
             (handler-timer/start {:params {:timer-id (str timer-id)}
                                   :user {:users/id (:member-id @test-context)}}))
          "fails with HTTP error when the timer is already started.")
      (is (= {:status 400
              :headers {}
              :body {:error "Timer does not exist."}}
             (handler-timer/start {:params {:timer-id (str (java.util.UUID/randomUUID))}
                                   :user {:users/id (:member-id @test-context)}}))
          "fails with HTTP error when the timer does not exist")
      (is (= {:status 400
              :headers {}
              :body {:error "Invalid Timer ID."}}
             (handler-timer/start {:params {:timer-id "42"}
                                   :user {:users/id (:member-id @test-context)}}))
          "fails with HTTP error when the timer id is invalid"))))

(deftest stop-timer-test
  (testing "stopping a timer"
    (let [member-id (:member-id @test-context)
          {timer-id :timers/id} (timer/create! db/datasource
                                               member-id
                                               (:task-id-1 @test-context)
                                               "A lovely timer.")
          _ (timer/start! db/datasource member-id timer-id)
          response (handler-timer/stop {:params {:timer-id (str timer-id)}
                                        :user {:users/id (:member-id @test-context)}})]
      (is (= 200
             (:status response))
          "the request succeeds")
      (is (s/valid? :time-spans/time-span
                    (:body response))
          "the body is a valid time span that was just stopped")
      (is (= {:status 400,
              :headers {},
              :body {:error "Timer is already stopped."}}
             (handler-timer/stop {:params {:timer-id (str timer-id)}
                                  :user {:users/id (:member-id @test-context)}}))
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
    (let [member-id (:member-id @test-context)
          task-id (:task-id-1 @test-context)
          _timers (dotimes [_ 3]
                    (->> (timer/create! db/datasource
                                        member-id
                                        task-id
                                        "A lovely timer.")
                         :timers/id
                         (timer/start! db/datasource member-id)))
          response (handler-timer/find-for-user-task {:params {:task-id (:task-id-1 @test-context)}
                                                      :user {:users/id (:member-id @test-context)}})]
      (is (= 200
             (:status response))
          "the request succeeds for an existing timer")
      (is (s/valid? :domain.timer/find-for-user-retval
                    (:body response))
          "the body of is a collection of timer objects, each with timer and time span information")
      (is (= {:status 404
              :headers {}
              :body {:error "Timers not found."}}
             (handler-timer/find-for-user-task {:params {:task-id Long/MAX_VALUE}
                                                :user {:users/id (:member-id @test-context)}}))
          "fails with HTTP error when timers do not exist"))))
