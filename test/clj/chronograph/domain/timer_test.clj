(ns chronograph.domain.timer-test
  (:require [chronograph.domain.timer :as timer]
            [clojure.test :refer :all]
            [chronograph.fixtures :as fixtures]
            [chronograph.factories :as factories]
            [chronograph.domain.acl :as acl]
            [chronograph.db.core :as db]
            [clojure.spec.alpha :as s]
            [chronograph.db.time-span :as db-time-span]
            [chronograph.utils.time :as time])
  (:import org.postgresql.util.PSQLException))

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


;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Create Timer Tests
;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(deftest create-timer-test
  (let [task-id (:task-id-1 @test-context)
        member-id (:member-id @test-context)
        organization-id (:organization-id @test-context)]
    (testing "Timer creation"
      (is (s/valid? :timers/timer
                    (timer/create! db/datasource
                                   organization-id
                                   member-id
                                   task-id
                                   nil))
          "without a note, returns a valid timer.")
      (is (s/valid? :timers/timer
                    (timer/create! db/datasource
                                   organization-id
                                   member-id
                                   task-id
                                   "A valid note."))
          "with a note, returns a valid timer.")
      (let [timer (timer/create! db/datasource
                                 organization-id
                                 member-id
                                 task-id
                                 "A valid note.")]
        (is (= (assoc timer
                      :time-spans [])
               (timer/find-for-user-by-timer-id db/datasource
                                                member-id
                                                (:timers/id timer)))
            "followed immediately by timer fetch, succeeds."))
      (is (every? (partial s/valid? :timers/timer)
                  (into (repeatedly 3 (fn [] (timer/create! db/datasource
                                                            organization-id
                                                            member-id
                                                            task-id
                                                            "A sample note.")))
                        (repeatedly 3 (fn [] (timer/create! db/datasource
                                                            organization-id
                                                            member-id
                                                            (:task-id-2 @test-context)
                                                            "A sample note.")))))
          "lets a member create multiple timers for multiple tasks.")
      (let [timer (timer/create! db/datasource
                                 organization-id
                                 member-id
                                 task-id
                                 "A sample note.")]
        (is (not (timer/running? db/datasource
                                 member-id
                                 (:timers/id timer)))
            "does not implicitly start the timer."))
      (let [task-of-other-org (factories/create-task
                               (factories/create-organization
                                (:users/id (factories/create-user))))]
        (is (nil? (timer/create! db/datasource
                                 organization-id
                                 member-id
                                 (:tasks/id task-of-other-org)
                                 "A valid note."))
            "does nothing when the task belonging to another organization."))
      (is (nil? (timer/create! db/datasource
                               organization-id
                               Long/MAX_VALUE
                               task-id
                               "A sample note."))
          "does nothing when the user does not exist.")
      (is (nil? (timer/create! db/datasource
                               organization-id
                               member-id
                               Long/MAX_VALUE
                               "A sample note."))
          "does nothing when the task does not exist."))))


;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Delete Timer Tests
;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(deftest delete-timer-test
  (testing "Timer deletion for timer without any time spans"
    (let [member-id (:member-id @test-context)
          organization-id (:organization-id @test-context)
          {timer-id :timers/id :as timer} (timer/create! db/datasource
                                                         organization-id
                                                         member-id
                                                         (:task-id-1 @test-context)
                                                         "A sample note.")]
      (is (= timer
             (timer/delete! db/datasource
                            member-id
                            timer-id))
          "returns the timer id if the timer exists.")
      (is (nil? (timer/delete! db/datasource
                               member-id
                               (java.util.UUID/randomUUID)))
          "returns nil if the timer does not exist."))))

(deftest delete-timer-having-time-spans-test
  (testing "When a timer having time spans is being deleted"
    (let [member-id (:member-id @test-context)
          organization-id (:organization-id @test-context)
          {timer-id :timers/id} (timer/create! db/datasource
                                               organization-id
                                               member-id
                                               (:task-id-1 @test-context)
                                               "A sample note.")]
      ;; first set up 3 time spans for the timer
      (dotimes [_ 3]
        (timer/start! db/datasource
                      member-id
                      timer-id)
        (timer/stop! db/datasource
                     member-id
                     timer-id))
      ;; then verify we get back 3 time spans for the timer
      (is (= 3
             (count (db-time-span/find-all-for-timer db/datasource timer-id)))
          "we can retrieve all associated time spans, prior to timer deletion.")
      ;; now, delete the timer
      (timer/delete! db/datasource member-id timer-id)
      ;; finally, verify the time spans are gone
      (is (empty?
           (db-time-span/find-all-for-timer db/datasource timer-id))
          "all its associated time spans are deleted."))))

(deftest delete-timer-isolation-by-user-test
  (let [admin-id (:owner-id @test-context)
        member-id (:member-id @test-context)
        task-id (:task-id-1 @test-context)
        organization-id (:organization-id @test-context)]
    (testing "Users trying to delete each others' timers."
      (let [timer-by-admin (timer/create! db/datasource
                                          organization-id
                                          admin-id
                                          task-id
                                          "A note by an admin.")
            timer-by-member (timer/create! db/datasource
                                           organization-id
                                           member-id
                                           task-id
                                           "A note by a member.")]
        (is (nil? (timer/delete! db/datasource
                                 member-id
                                 (:timers/id timer-by-admin)))
            "A member cannot delete an admin's timer.")
        (is (nil? (timer/delete! db/datasource
                                 admin-id
                                 (:timers/id timer-by-member)))
            "An admin cannot delete a member's timer.")))))


;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Update Timer's Note Tests
;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(deftest update-timer-note-test
  (testing "updating a Timer's note"
    (with-redefs [time/now (constantly (time/now))]
      (let [member-id (:member-id @test-context)
            organization-id (:organization-id @test-context)
            {timer-id :timers/id :as timer} (timer/create! db/datasource
                                                           organization-id
                                                           member-id
                                                           (:task-id-1 @test-context)
                                                           "A sample note.")
            revised-note (str "Revised note " (rand))]
        (is (= (assoc timer
                      :timers/note
                      revised-note)
               (timer/update-note! db/datasource
                                          member-id
                                          timer-id
                                          revised-note))
            "returns a Timer containing the updated note.")
        (is (nil? (timer/update-note! db/datasource
                                      member-id
                                      (java.util.UUID/randomUUID)
                                      revised-note))
            "does nothing if the timer does not exist.")))))

(deftest update-note-isolation-by-user-test
  (let [admin-id (:owner-id @test-context)
        member-id (:member-id @test-context)
        task-id (:task-id-1 @test-context)
        organization-id (:organization-id @test-context)]
    (testing "Users trying to update each others' timer notes."
      (let [unstarted-timer-by-admin (timer/create! db/datasource
                                                    organization-id
                                                    admin-id
                                                    task-id
                                                    "A note by an admin.")
            unstarted-timer-by-member (timer/create! db/datasource
                                                     organization-id
                                                     member-id
                                                     task-id
                                                     "A note by a member.")]
        (is (nil? (timer/update-note! db/datasource
                                      member-id
                                      (:timers/id unstarted-timer-by-admin)
                                      "A sneaky note by a member."))
            "A member cannot overwrite an admin's note.")
        (is (nil? (timer/update-note! db/datasource
                                      admin-id
                                      (:timers/id unstarted-timer-by-member)
                                      "A sneaky note by an admin."))
            "An admin cannot overwrite a member's note.")))))


;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Start Timer Tests
;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(deftest start-timer-test
  (testing "starting a timer"
    (let [member-id (:member-id @test-context)
          organization-id (:organization-id @test-context)
          timer (timer/create! db/datasource
                               organization-id
                               member-id
                               (:task-id-1 @test-context)
                               "A sample note.")]
      (is (s/valid? :time-spans/time-span
                    (timer/start! db/datasource
                                  member-id
                                  (:timers/id timer)))
          "returns a valid TimeSpan. This TimeSpan represents the currently-running state of the timer.")
      (is (nil? (timer/start! db/datasource
                              member-id
                              (:timers/id timer)))
          "does nothing. An already-started timer remains started."))))

(deftest start-timer-isolation-by-user-test
  (let [admin-id (:owner-id @test-context)
        member-id (:member-id @test-context)
        task-id (:task-id-1 @test-context)
        organization-id (:organization-id @test-context)]
    (testing "Users trying to start each others' timers."
      (let [unstarted-timer-by-admin (timer/create! db/datasource
                                                    organization-id
                                                    admin-id
                                                    task-id
                                                    "A note by an admin.")
            unstarted-timer-by-member (timer/create! db/datasource
                                                     organization-id
                                                     member-id
                                                     task-id
                                                     "A note by a member.")]
        (is (nil? (timer/start! db/datasource
                                member-id
                                (:timers/id unstarted-timer-by-admin)))
            "A member cannot start an admin's timer.")
        (is (nil? (timer/start! db/datasource
                                admin-id
                                (:timers/id unstarted-timer-by-member)))
            "An admin cannot start a member's timer.")))))


;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Stop Timer Tests
;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(deftest stop-timer-test
  (testing "stopping an unstarted timer (having no time spans)"
    (let [member-id (:member-id @test-context)
          organization-id (:organization-id @test-context)]
      (is (nil? (timer/stop! db/datasource
                             member-id
                             (:timers/id (timer/create! db/datasource
                                                        organization-id
                                                        member-id
                                                        (:task-id-1 @test-context)
                                                        "A sample note."))))
          "does nothing. An un-started timer remains un-started.")))

  (testing "stopping a running timer (having at least one time span)"
    (let [member-id (:member-id @test-context)
          organization-id (:organization-id @test-context)
          timer (let [timer (timer/create! db/datasource
                                           organization-id
                                           member-id
                                           (:task-id-1 @test-context)
                                           "A sample note.")]
                  (timer/start! db/datasource
                                member-id
                                (:timers/id timer))
                  timer)]
      (is (s/valid? :time-spans/time-span
                    (timer/stop! db/datasource
                                 member-id
                                 (:timers/id timer)))
          "returns a valid TimeSpan. This TimeSpan represents the just-stopped state of the timer.")
      (is (nil? (timer/stop! db/datasource
                             member-id
                             (:timers/id timer)))
          "does nothing. A stopped timer remain stopped."))))

(deftest stop-timer-isolation-by-user-test
  (let [admin-id (:owner-id @test-context)
        member-id (:member-id @test-context)
        task-id (:task-id-1 @test-context)
        organization-id (:organization-id @test-context)]
    (testing "Users trying to stop each others' timers."
      (let [running-timer-by-admin (let [timer (timer/create! db/datasource
                                                              organization-id
                                                              admin-id
                                                              task-id
                                                              "A note by an admin.")]
                                     (timer/start! db/datasource
                                                   admin-id
                                                   (:timers/id timer))
                                     timer)
            running-timer-by-member (let [timer (timer/create! db/datasource
                                                               organization-id
                                                               member-id
                                                               task-id
                                                               "A note by an admin.")]
                                      (timer/start! db/datasource
                                                    member-id
                                                    (:timers/id timer))
                                      timer)]
        (is (nil? (timer/stop! db/datasource
                               member-id
                               (:timers/id running-timer-by-admin)))
            "A member cannot stop an admin's timer.")
        (is (nil? (timer/stop! db/datasource
                               admin-id
                               (:timers/id running-timer-by-member)))
            "An admin cannot start a member's timer.")))))


;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Find Timer Tests
;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(deftest find-timer-by-timer-id
  (testing "when we find a timer by timer id, and it was started and/or stopped at least once"
    (let [member-id (:member-id @test-context)
          organization-id (:organization-id @test-context)
          {timer-id :timers/id} (timer/create! db/datasource
                                               organization-id
                                               member-id
                                               (:task-id-1 @test-context)
                                               "A sample note.")]
      ;; start/stop the timer a few times
      (dotimes [_ 3]
        (timer/start! db/datasource
                      member-id
                      timer-id)
        (timer/stop! db/datasource
                     member-id
                     timer-id))
      ;; validate timer data retrieved
      (is (s/valid? :domain.timer/find-by-id-retval
                    (timer/find-for-user-by-timer-id db/datasource
                                                     member-id
                                                     timer-id))
          "we get back a valid Timer object. The object contains Timer data, as well as all the TimeSpans associated with the Timer.")))

  (testing "when we find a timer by timer id, and it was never started"
    (let [member-id (:member-id @test-context)
          organization-id (:organization-id @test-context)
          {timer-id :timers/id} (timer/create! db/datasource
                                               organization-id
                                               member-id
                                               (:task-id-1 @test-context)
                                               "A sample note.")
          timer (timer/find-for-user-by-timer-id db/datasource
                                                 member-id
                                                 timer-id)]
      (is (s/valid? :domain.timer/find-by-id-retval
                    timer)
          "we get back a valid Timer object.")
      (is (= []
             (:time-spans timer))
          "the Timer has no TimeSpans (empty collection) associated with it."))))

(deftest find-timer-for-user
  (testing "when we find timers for a member's task"
    (let [member-id (:member-id @test-context)
          organization-id (:organization-id @test-context)]
      (dotimes [_ 3]
        (->> (timer/create! db/datasource
                            organization-id
                            member-id
                            (:task-id-1 @test-context)
                            "A sample note.")
             :timers/id
             (timer/start! db/datasource member-id)
             :time-spans/timer-id
             (timer/stop! db/datasource member-id)
             :time-spans/timer-id
             (timer/start! db/datasource member-id)))

      (is (= 3 (count (timer/find-for-user-task db/datasource
                                                member-id
                                                (:task-id-1 @test-context))))
          "we find all the associated Timer objects")
      (is (s/valid? :domain.timer/find-for-user-retval
                    (timer/find-for-user-task db/datasource
                                              member-id
                                              (:task-id-1 @test-context)))
          "each Timer object has Timer information as well as TimeSpan information."))))

(deftest find-timer-isolation-by-user-test
  (let [admin-id (:owner-id @test-context)
        member-id (:member-id @test-context)
        organization-id (:organization-id @test-context)
        task-id-timed-by-admin (:task-id-1 @test-context)
        task-id-timed-by-member (:task-id-2 @test-context)]
    (testing "Users trying to read each others' timers."
      (let [timer-for-task-timed-by-admin (timer/create! db/datasource
                                                         organization-id
                                                         admin-id
                                                         task-id-timed-by-admin
                                                         "A note by an admin.")
            timer-for-task-timed-by-member (timer/create! db/datasource
                                                          organization-id
                                                          member-id
                                                          task-id-timed-by-member
                                                          "A note by a member.")]
        (is (nil? (timer/find-for-user-by-timer-id db/datasource
                                                   member-id
                                                   (:timers/id timer-for-task-timed-by-admin)))
            "A member cannot fetch an admin's timers by timer id.")
        (is (nil? (timer/find-for-user-by-timer-id db/datasource
                                                   admin-id
                                                   (:timers/id timer-for-task-timed-by-member)))
            "An admin cannot fetch a member's timer by timer id.")
        (is (nil? (timer/find-for-user-task db/datasource
                                            member-id
                                            task-id-timed-by-admin))
            "A member cannot fetch admin's timers by task id.")
        (is (nil? (timer/find-for-user-task db/datasource
                                            admin-id
                                            task-id-timed-by-member))
            "An admin cannot fetch a member's timers by task id.")))))
