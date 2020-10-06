(ns chronograph.domain.timer-test
  (:require [chronograph.domain.timer :as timer]
            [clojure.test :refer :all]
            [chronograph.fixtures :as fixtures]
            [chronograph.factories :as factories]
            [chronograph.domain.acl :as acl]
            [chronograph.db.core :as db]
            [clojure.spec.alpha :as s]
            [chronograph.db.time-span :as db-time-span]
            [chronograph.utils.time :as time]))

(defn- setup-org-users-tasks!
  []
  (let [{user-1-id :users/id}
        (factories/create-user)

        {organization-id :organizations/id
         :as organization}
        (factories/create-organization user-1-id)

        {user-2-id :users/id} (factories/create-user)
        _ (acl/create! db/datasource
                       {:user-id user-2-id
                        :organization-id organization-id
                        :role acl/member})
        {task-id-1 :tasks/id} (factories/create-task organization)
        {task-id-2 :tasks/id} (factories/create-task organization)]
    {:user-1-id user-1-id
     :organization-id organization-id
     :user-2-id user-2-id
     :task-id-1 task-id-1
     :task-id-2 task-id-2}))

(use-fixtures :once fixtures/config fixtures/datasource)
(use-fixtures :each fixtures/clear-db)


;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Create Timer Tests
;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest create-timer-success-test
  (testing "Timer creation"
    (let [test-context (setup-org-users-tasks!)
          task-id (:task-id-1 test-context)
          user-id (:user-2-id test-context)
          organization-id (:organization-id test-context)]
      (is (s/valid? :timers/timer
                    (timer/create! db/datasource
                                   organization-id
                                   user-id
                                   task-id
                                   nil))
          "without a note, returns a valid timer.")
      (is (s/valid? :timers/timer
                    (timer/create! db/datasource
                                   organization-id
                                   user-id
                                   task-id
                                   "A valid note."))
          "with a note, returns a valid timer.")
      (let [timer (timer/create! db/datasource
                                 organization-id
                                 user-id
                                 task-id
                                 "A valid note.")]
        (is (= (assoc timer
                      :time-spans [])
               (timer/find-for-user-by-timer-id db/datasource
                                                user-id
                                                (:timers/id timer)))
            "followed immediately by timer fetch, succeeds."))
      (is (every? (partial s/valid? :timers/timer)
                  (into (repeatedly 3 (fn [] (timer/create! db/datasource
                                                            organization-id
                                                            user-id
                                                            task-id
                                                            "A sample note.")))
                        (repeatedly 3 (fn [] (timer/create! db/datasource
                                                            organization-id
                                                            user-id
                                                            task-id
                                                            "A sample note.")))))
          "lets a user create multiple timers for multiple tasks."))))

(deftest create-timer-disallowed-test
  (testing "Timer creation is disallowed when"
    (let [test-context (setup-org-users-tasks!)
          task-id (:task-id-1 test-context)
          user-id (:user-2-id test-context)
          organization-id (:organization-id test-context)
          task-of-other-org (factories/create-task
                             (factories/create-organization
                              (:users/id (factories/create-user))))]
      (is (nil? (timer/create! db/datasource
                               organization-id
                               user-id
                               (:tasks/id task-of-other-org)
                               "A valid note."))
          "the task belongs to another organization.")
      (is (nil? (timer/create! db/datasource
                               organization-id
                               Long/MAX_VALUE
                               task-id
                               "A sample note."))
          "the user does not exist.")
      (is (nil? (timer/create! db/datasource
                               organization-id
                               user-id
                               Long/MAX_VALUE
                               "A sample note."))
          "the task does not exist."))))


;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Delete Timer Tests
;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(deftest delete-timer-test
  (testing "Timer deletion for timer without any time spans"
    (let [test-context (setup-org-users-tasks!)
          user-2-id (:user-2-id test-context)
          organization-id (:organization-id test-context)
          {timer-id :timers/id :as timer} (timer/create! db/datasource
                                                         organization-id
                                                         user-2-id
                                                         (:task-id-1 test-context)
                                                         "A sample note.")]
      (is (= timer
             (timer/delete! db/datasource
                            user-2-id
                            timer-id))
          "returns the timer id if the timer exists.")
      (is (nil? (timer/delete! db/datasource
                               user-2-id
                               (java.util.UUID/randomUUID)))
          "returns nil if the timer does not exist."))))

(deftest delete-timer-having-time-spans-test
  (testing "When a timer having time spans is being deleted"
    (let [test-context (setup-org-users-tasks!)
          user-2-id (:user-2-id test-context)
          organization-id (:organization-id test-context)
          {timer-id :timers/id} (timer/create! db/datasource
                                               organization-id
                                               user-2-id
                                               (:task-id-1 test-context)
                                               "A sample note.")]
      ;; first set up 3 time spans for the timer
      (dotimes [_ 3]
        (timer/start! db/datasource
                      user-2-id
                      timer-id)
        (timer/stop! db/datasource
                     user-2-id
                     timer-id))
      ;; then verify we get back 3 time spans for the timer
      (is (= 3
             (count (db-time-span/find-all-for-timer db/datasource timer-id)))
          "we can retrieve all associated time spans, prior to timer deletion.")
      ;; now, delete the timer
      (timer/delete! db/datasource user-2-id timer-id)
      ;; finally, verify the time spans are gone
      (is (empty?
           (db-time-span/find-all-for-timer db/datasource timer-id))
          "all its associated time spans are deleted."))))

(deftest delete-timer-isolation-by-user-test
  (testing "Users trying to delete each others' timers."
    (let [test-context (setup-org-users-tasks!)
          user-1-id (:user-1-id test-context)
          user-2-id (:user-2-id test-context)
          task-id (:task-id-1 test-context)
          organization-id (:organization-id test-context)
          timer-by-user-1 (timer/create! db/datasource
                                         organization-id
                                         user-1-id
                                         task-id
                                         "A note by user-1.")
          timer-by-user-2 (timer/create! db/datasource
                                         organization-id
                                         user-2-id
                                         task-id
                                         "A note by user-2.")]
      (is (nil? (timer/delete! db/datasource
                               user-2-id
                               (:timers/id timer-by-user-1)))
          "user-2 cannot delete user-1's timer.")
      (is (nil? (timer/delete! db/datasource
                               user-1-id
                               (:timers/id timer-by-user-2)))
          "user-1 cannot delete user-2's timer."))))


;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Update Timer's Note Tests
;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(deftest update-timer-note-test
  (testing "updating a Timer's note"
    (with-redefs [time/now (constantly (time/now))]
      (let [test-context (setup-org-users-tasks!)
            user-2-id (:user-2-id test-context)
            organization-id (:organization-id test-context)
            {timer-id :timers/id :as timer} (timer/create! db/datasource
                                                           organization-id
                                                           user-2-id
                                                           (:task-id-1 test-context)
                                                           "A sample note.")
            revised-note (str "Revised note " (rand))]
        (is (= (assoc timer
                      :timers/note
                      revised-note)
               (timer/update-note! db/datasource
                                   user-2-id
                                   timer-id
                                   revised-note))
            "returns a Timer containing the updated note.")
        (is (nil? (timer/update-note! db/datasource
                                      user-2-id
                                      (java.util.UUID/randomUUID)
                                      revised-note))
            "does nothing if the timer does not exist.")))))

(deftest update-note-isolation-by-user-test
  (testing "Users trying to update each others' timer notes."
    (let [test-context (setup-org-users-tasks!)
          user-1-id (:user-1-id test-context)
          user-2-id (:user-2-id test-context)
          task-id (:task-id-1 test-context)
          organization-id (:organization-id test-context)
          unstarted-timer-by-user-1 (timer/create! db/datasource
                                                   organization-id
                                                   user-1-id
                                                   task-id
                                                   "A note by user-1.")
          unstarted-timer-by-user-2 (timer/create! db/datasource
                                                   organization-id
                                                   user-2-id
                                                   task-id
                                                   "A note by user-2.")]
      (is (nil? (timer/update-note! db/datasource
                                    user-2-id
                                    (:timers/id unstarted-timer-by-user-1)
                                    "A sneaky note by user-2."))
          "user-2 cannot overwrite user-1's note.")
      (is (nil? (timer/update-note! db/datasource
                                    user-1-id
                                    (:timers/id unstarted-timer-by-user-2)
                                    "A sneaky note by user-1."))
          "user-1 cannot overwrite user-2's note."))))


;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Start Timer Tests
;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(deftest start-timer-test
  (testing "starting a timer"
    (let [test-context (setup-org-users-tasks!)
          user-id (:user-2-id test-context)
          organization-id (:organization-id test-context)
          timer (timer/create! db/datasource
                               organization-id
                               user-id
                               (:task-id-1 test-context)
                               "A sample note.")]
      (is (s/valid? :time-spans/time-span
                    (timer/start! db/datasource
                                  user-id
                                  (:timers/id timer)))
          "returns a valid TimeSpan. This TimeSpan represents the currently-running state of the timer.")
      (is (nil? (timer/start! db/datasource
                              user-id
                              (:timers/id timer)))
          "does nothing. An already-started timer remains started."))))

(deftest start-timer-isolation-by-user-test
  (testing "Users trying to start each others' timers."
    (let [test-context (setup-org-users-tasks!)
          user-1-id (:user-1-id test-context)
          user-2-id (:user-2-id test-context)
          task-id (:task-id-1 test-context)
          organization-id (:organization-id test-context)
          unstarted-timer-by-user-1 (timer/create! db/datasource
                                                   organization-id
                                                   user-1-id
                                                   task-id
                                                   "A note by user-1.")
          unstarted-timer-by-user-2 (timer/create! db/datasource
                                                   organization-id
                                                   user-2-id
                                                   task-id
                                                   "A note by user-2.")]
      (is (nil? (timer/start! db/datasource
                              user-2-id
                              (:timers/id unstarted-timer-by-user-1)))
          "user-2 cannot start user-1's timer.")
      (is (nil? (timer/start! db/datasource
                              user-1-id
                              (:timers/id unstarted-timer-by-user-2)))
          "user-1 cannot start user-2's timer."))))


;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Stop Timer Tests
;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest stop-unstarted-timer-test
  (testing "stopping an unstarted timer (having no time spans)"
    (let [test-context (setup-org-users-tasks!)
          user-id (:user-2-id test-context)
          organization-id (:organization-id test-context)]
      (is (nil? (timer/stop! db/datasource
                             user-id
                             (:timers/id (timer/create! db/datasource
                                                        organization-id
                                                        user-id
                                                        (:task-id-1 test-context)
                                                        "A sample note."))))
          "does nothing. An un-started timer remains un-started."))))

(deftest stop-running-timer-test
  (testing "stopping a running timer (having at least one time span)"
    (let [test-context (setup-org-users-tasks!)
          user-id (:user-2-id test-context)
          organization-id (:organization-id test-context)
          timer (let [timer (timer/create! db/datasource
                                           organization-id
                                           user-id
                                           (:task-id-1 test-context)
                                           "A sample note.")]
                  (timer/start! db/datasource
                                user-id
                                (:timers/id timer))
                  timer)]
      (is (s/valid? :time-spans/time-span
                    (timer/stop! db/datasource
                                 user-id
                                 (:timers/id timer)))
          "returns a valid TimeSpan. This TimeSpan represents the just-stopped state of the timer.")
      (is (nil? (timer/stop! db/datasource
                             user-id
                             (:timers/id timer)))
          "does nothing. A stopped timer remain stopped."))))

(deftest stop-timer-isolation-by-user-test
  (testing "Users trying to stop each others' timers."
    (let [test-context (setup-org-users-tasks!)
          user-1-id (:user-1-id test-context)
          user-2-id (:user-2-id test-context)
          task-id (:task-id-1 test-context)
          organization-id (:organization-id test-context)
          running-timer-by-user-1 (let [timer (timer/create! db/datasource
                                                             organization-id
                                                             user-1-id
                                                             task-id
                                                             "A note by user-1.")]
                                    (timer/start! db/datasource
                                                  user-1-id
                                                  (:timers/id timer))
                                    timer)
          running-timer-by-user-2 (let [timer (timer/create! db/datasource
                                                             organization-id
                                                             user-2-id
                                                             task-id
                                                             "A note by user-1.")]
                                    (timer/start! db/datasource
                                                  user-2-id
                                                  (:timers/id timer))
                                    timer)]
      (is (nil? (timer/stop! db/datasource
                             user-2-id
                             (:timers/id running-timer-by-user-1)))
          "user-2 cannot stop user-1's timer.")
      (is (nil? (timer/stop! db/datasource
                             user-1-id
                             (:timers/id running-timer-by-user-2)))
          "user-1 cannot start user-2's timer."))))


;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Find Timer Tests
;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest find-unstarted-timer-by-timer-id
  (testing "when we find a timer by timer id, and it was never started"
    (let [test-context (setup-org-users-tasks!)
          user-id (:user-2-id test-context)
          organization-id (:organization-id test-context)
          {timer-id :timers/id} (timer/create! db/datasource
                                               organization-id
                                               user-id
                                               (:task-id-1 test-context)
                                               "A sample note.")
          timer (timer/find-for-user-by-timer-id db/datasource
                                                 user-id
                                                 timer-id)]
      (is (s/valid? :domain.timer/find-by-id-retval
                    timer)
          "we get back a valid Timer object.")
      (is (= []
             (:time-spans timer))
          "the Timer has no TimeSpans (empty collection) associated with it."))))

(deftest find-started-timer-by-timer-id
  (testing "when we find a timer by timer id, and it was started and/or stopped at least once"
    (let [test-context (setup-org-users-tasks!)
          user-id (:user-2-id test-context)
          organization-id (:organization-id test-context)
          {timer-id :timers/id} (timer/create! db/datasource
                                               organization-id
                                               user-id
                                               (:task-id-1 test-context)
                                               "A sample note.")]
      ;; start/stop the timer a few times
      (dotimes [_ 3]
        (timer/start! db/datasource
                      user-id
                      timer-id)
        (timer/stop! db/datasource
                     user-id
                     timer-id))
      ;; validate timer data retrieved
      (is (s/valid? :domain.timer/find-by-id-retval
                    (timer/find-for-user-by-timer-id db/datasource
                                                     user-id
                                                     timer-id))
          "we get back a valid Timer object. The object contains Timer data, as well as all the TimeSpans associated with the Timer."))))

(deftest find-timer-for-user
  (testing "when we find timers for user-2's task"
    (let [test-context (setup-org-users-tasks!)
          user-id (:user-2-id test-context)
          organization-id (:organization-id test-context)]
      (dotimes [_ 3]
        (->> (timer/create! db/datasource
                            organization-id
                            user-id
                            (:task-id-1 test-context)
                            "A sample note.")
             :timers/id
             (timer/start! db/datasource user-id)
             :time-spans/timer-id
             (timer/stop! db/datasource user-id)
             :time-spans/timer-id
             (timer/start! db/datasource user-id)))

      (is (= 3 (count (timer/find-for-user-task db/datasource
                                                user-id
                                                (:task-id-1 test-context))))
          "we find all the associated Timer objects")
      (is (s/valid? :domain.timer/find-for-user-retval
                    (timer/find-for-user-task db/datasource
                                              user-id
                                              (:task-id-1 test-context)))
          "each Timer object has Timer information as well as TimeSpan information."))))

(deftest find-timer-isolation-by-user-test
  (testing "Users trying to read each others' timers."
    (let [test-context (setup-org-users-tasks!)
          user-1-id (:user-1-id test-context)
          user-2-id (:user-2-id test-context)
          organization-id (:organization-id test-context)
          task-id-timed-by-user-1 (:task-id-1 test-context)
          task-id-timed-by-user-2 (:task-id-2 test-context)
          timer-for-task-timed-by-user-1 (timer/create! db/datasource
                                                        organization-id
                                                        user-1-id
                                                        task-id-timed-by-user-1
                                                        "A note by user-1.")
          timer-for-task-timed-by-user-2 (timer/create! db/datasource
                                                        organization-id
                                                        user-2-id
                                                        task-id-timed-by-user-2
                                                        "A note by user-2.")]
      (is (nil? (timer/find-for-user-by-timer-id db/datasource
                                                 user-2-id
                                                 (:timers/id timer-for-task-timed-by-user-1)))
          "user-2 cannot fetch user-1's timers by timer id.")
      (is (nil? (timer/find-for-user-by-timer-id db/datasource
                                                 user-1-id
                                                 (:timers/id timer-for-task-timed-by-user-2)))
          "user-1 cannot fetch user-2's timer by timer id.")
      (is (nil? (timer/find-for-user-task db/datasource
                                          user-2-id
                                          task-id-timed-by-user-1))
          "user-2 cannot fetch user-1's timers by task id.")
      (is (nil? (timer/find-for-user-task db/datasource
                                          user-1-id
                                          task-id-timed-by-user-2))
          "user-1 cannot fetch user-2's timers by task id."))))
