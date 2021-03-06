(ns chronograph.domain.timer-test
  (:require [chronograph.domain.timer :as timer]
            [clojure.test :refer :all]
            [chronograph.test-utils :as tu]
            [chronograph.fixtures :as fixtures]
            [chronograph.factories :as factories]
            [chronograph.domain.acl :as acl]
            [chronograph.db.core :as db]
            [clojure.spec.alpha :as s]
            [chronograph.utils.time :as time]
            [next.jdbc :as jdbc])
  (:import [java.time LocalDate Instant]))

(defn- setup-org-users-tasks!
  []
  (let [{admin-id :users/id}
        (factories/create-user)

        {organization-id :organizations/id
         :as organization}
        (factories/create-organization admin-id)

        {user-id :users/id} (factories/create-user)
        _ (acl/create! db/datasource
                       #:acls{:user-id user-id
                              :organization-id organization-id
                              :role acl/member})
        {task-id-1 :tasks/id :as task-1} (factories/create-task organization)
        {task-id-2 :tasks/id :as task-2} (factories/create-task organization)]
    {:admin-id admin-id
     :organization-id organization-id
     :user-id user-id
     :task-1 task-1
     :task-2 task-2
     :task-id-1 task-id-1
     :task-id-2 task-id-2}))

(use-fixtures :once fixtures/config fixtures/datasource)
(use-fixtures :each fixtures/clear-db)

(def default-local-date (LocalDate/parse "2020-01-14"))

;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Create Timer Tests
;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(deftest create-timer-success-test
  (testing "Timer creation"
    (let [{:keys [task-id-1 user-id organization-id]} (setup-org-users-tasks!)
          task-id task-id-1]
      (is (s/valid? :timers/timer
                    (timer/create! db/datasource
                                   organization-id
                                   #:timers{:user-id user-id
                                            :task-id task-id
                                            :recorded-for default-local-date}))
          "without a note, returns a valid timer.")
      (is (s/valid? :timers/timer
                    (timer/create! db/datasource
                                   organization-id
                                   #:timers{:user-id user-id
                                            :task-id task-id
                                            :note "A valid note."
                                            :recorded-for default-local-date}))
          "with a note, returns a valid timer.")
      (is (apply = ((juxt :timers/created-at :timers/updated-at)
                    (timer/create! db/datasource
                                   organization-id
                                   #:timers{:user-id user-id
                                            :task-id task-id
                                            :note "A valid note."
                                            :recorded-for default-local-date})))
          "Has created_at = updated_at when just created.")
      (let [timer (timer/create! db/datasource
                                 organization-id
                                 #:timers{:user-id user-id
                                          :task-id task-id
                                          :note "A valid note."
                                          :recorded-for default-local-date})]
        (is (= #:timers{:user-id      user-id
                        :task-id      task-id
                        :id           (:timers/id timer)
                        :note         "A valid note."
                        :recorded-for default-local-date
                        :time-spans   []}
               (-> (timer/find-by-user-and-id db/datasource
                                              user-id
                                              (:timers/id timer))
                   (dissoc :timers/created-at :timers/updated-at)))
            "Create followed immediately by timer fetch, succeeds."))
      (is (every? (partial s/valid? :timers/timer)
                  (into (repeatedly 3 (fn [] (timer/create! db/datasource
                                                            organization-id
                                                            #:timers{:user-id user-id
                                                                     :task-id task-id
                                                                     :note "A sample note."
                                                                     :recorded-for default-local-date})))
                        (repeatedly 3 (fn [] (timer/create! db/datasource
                                                            organization-id
                                                            #:timers{:user-id user-id
                                                                     :task-id task-id
                                                                     :note "A sample note."
                                                                     :recorded-for default-local-date})))))
          "lets a user create multiple timers for multiple tasks."))))

(deftest create-timer-disallowed-test
  (testing "Timer creation is disallowed when"
    (let [{:keys [user-id organization-id task-id-1]} (setup-org-users-tasks!)
          task-of-other-org (factories/create-task
                             (factories/create-organization
                              (:users/id (factories/create-user))))]
      (is (nil? (timer/create! db/datasource
                               organization-id
                               #:timers{:user-id user-id
                                        :task-id (:tasks/id task-of-other-org)
                                        :note "A valid note."
                                        :recorded-for default-local-date}))
          "the task belongs to another organization.")
      (is (nil? (timer/create! db/datasource
                               organization-id
                               #:timers{:user-id Long/MAX_VALUE
                                        :task-id task-id-1
                                        :note "A sample note."
                                        :recorded-for default-local-date}))
          "the user does not exist.")
      (is (nil? (timer/create! db/datasource
                               organization-id
                               #:timers{:user-id user-id
                                        :task-id Long/MAX_VALUE
                                        :note "A sample note."
                                        :recorded-for default-local-date}))
          "the task does not exist."))))

(deftest create-and-start!-test
  (testing "Should create a running timer when called"
    (let [{:keys [task-id-1 user-id organization-id]} (setup-org-users-tasks!)
          timer (jdbc/with-transaction [tx db/datasource]
                  (timer/create-and-start! tx organization-id #:timers{:user-id      user-id
                                                                       :task-id      task-id-1
                                                                       :note         "A sample note."
                                                                       :recorded-for default-local-date}))]
      (is (timer/running? timer)))))

;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Delete Timer Tests
;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(deftest delete-timer-test
  (testing "Timer deletion for timer without any time spans"
    (let [{:keys [user-id organization-id task-id-1]} (setup-org-users-tasks!)
          {timer-id :timers/id :as timer} (factories/create-timer organization-id
                                                                  #:timers{:user-id user-id
                                                                           :task-id task-id-1
                                                                           :note "A sample note."})]
      (is (nil? (timer/delete! db/datasource
                               user-id
                               (java.util.UUID/randomUUID)))
          "returns nil if the timer does not exist.")
      (is (= timer
             (timer/delete! db/datasource
                            user-id
                            timer-id))
          "returns the timer object if the timer exists.")
      (is (nil? (timer/find-by-user-and-id db/datasource
                                           user-id
                                           timer-id))
          "ceases to exist in DB, and cannot be fetched."))))

(deftest delete-timer-having-time-spans-test
  (testing "When a timer having time spans is being deleted"
    (let [{:keys [user-id organization-id task-id-1]} (setup-org-users-tasks!)
          {timer-id :timers/id} (factories/create-timer organization-id
                                                        #:timers{:user-id user-id
                                                                 :task-id task-id-1
                                                                 :note "A sample note."})]
      ;; first set up 3 time spans for the timer
      (dotimes [_ 3]
        (timer/start! db/datasource
                      user-id
                      timer-id)
        (timer/stop! db/datasource
                     user-id
                     timer-id))
      ;; now, delete the timer
      (timer/delete! db/datasource user-id timer-id)
      ;; then, verify the timer is gone
      (is (nil? (timer/find-by-user-and-id db/datasource
                                           user-id
                                           timer-id))
          "the timer ceases to exist in DB, and cannot be fetched."))))

(deftest delete-timer-isolation-by-user-test
  (testing "Users trying to delete each others' timers."
    (let [{:keys [admin-id user-id task-id-1 organization-id]} (setup-org-users-tasks!)
          task-id task-id-1
          timer-by-user-1 (factories/create-timer organization-id
                                                  #:timers{:user-id admin-id
                                                           :task-id task-id
                                                           :note "A note by user-1."})
          timer-by-user-2 (factories/create-timer organization-id
                                                  #:timers{:user-id user-id
                                                           :task-id task-id
                                                           :note "A note by user-2."})]
      (is (nil? (timer/delete! db/datasource
                               user-id
                               (:timers/id timer-by-user-1)))
          "user-2 cannot delete user-1's timer.")
      (is (nil? (timer/delete! db/datasource
                               admin-id
                               (:timers/id timer-by-user-2)))
          "user-1 cannot delete user-2's timer."))))

;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Update Timer Tests
;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(deftest update!-test
  (testing "Updating a stopped timer"
    (tu/with-fixtures [fixtures/clear-db]
      (with-redefs [time/now (constantly (Instant/parse "2020-11-30T11:30:00Z"))]
        (let [{:keys [user-id organization-id task-id-1 task-id-2]} (setup-org-users-tasks!)
              {:timers/keys [id]} (factories/create-timer organization-id
                                                          #:timers{:user-id user-id
                                                                   :task-id task-id-1
                                                                   :note    "A note by user-1."})
              updated-timer   (jdbc/with-transaction [tx db/datasource]
                                (timer/update! tx id {:duration-in-secs 1800
                                                      :note             "Updated note"
                                                      :task-id          task-id-2}))
              retrieved-timer (timer/find-by-user-and-id db/datasource user-id id)]
          (is (= updated-timer retrieved-timer)
              "The updated timer should be returned")
          (is (= #:timers{:task-id    task-id-2
                          :note       "Updated note"
                          :time-spans [{:started-at (Instant/parse "2020-11-30T11:00:00Z")
                                        :stopped-at (Instant/parse "2020-11-30T11:30:00Z")}]}
                 (select-keys retrieved-timer [:timers/task-id :timers/note :timers/time-spans]))
              "The fields should be updated")))))

  (testing "Updating a started timer"
    (tu/with-fixtures [fixtures/clear-db]
      (with-redefs [time/now (constantly (Instant/parse "2020-11-30T11:30:00Z"))]
        (let [{:keys [user-id organization-id task-id-1 task-id-2]} (setup-org-users-tasks!)
              {:timers/keys [id]} (factories/create-timer organization-id
                                                          #:timers{:user-id user-id
                                                                   :task-id task-id-1
                                                                   :note    "A note by user-1."})
              _               (timer/start! db/datasource user-id id)
              updated-timer   (jdbc/with-transaction [tx db/datasource]
                                (timer/update! tx id {:duration-in-secs 1800
                                                      :note             "Updated note"
                                                      :task-id          task-id-2}))
              retrieved-timer (timer/find-by-user-and-id db/datasource user-id id)]
          (is (= updated-timer retrieved-timer)
              "The updated timer should be returned")
          (is (= #:timers{:task-id    task-id-2
                          :note       "Updated note"
                          :time-spans [{:started-at (Instant/parse "2020-11-30T11:00:00Z")
                                        :stopped-at nil}]}
                 (select-keys retrieved-timer [:timers/task-id :timers/note :timers/time-spans]))
              "The fields should be updated"))))))

;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Start Timer Tests
;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(deftest start-timer-test
  (testing "starting a timer"
    (let [{:keys [user-id organization-id task-id-1]} (setup-org-users-tasks!)
          timer (factories/create-timer organization-id
                                        #:timers{:user-id user-id
                                                 :task-id task-id-1
                                                 :note "A sample note."})]
      (is (s/valid? :timers/timer
                    (timer/start! db/datasource
                                  user-id
                                  (:timers/id timer)))
          "returns a valid timer.")
      (is (nil? (timer/start! db/datasource
                              user-id
                              (:timers/id timer)))
          "does nothing. An already-started timer remains started."))))

(deftest start-timer-when-another-timer-is-running-test
  (testing "starting a timer should stop other running timers belonging to the user"
    (let [{:keys [user-id organization-id task-id-1]} (setup-org-users-tasks!)
          timer1 (factories/create-timer organization-id
                                         #:timers{:user-id user-id
                                                  :task-id task-id-1
                                                  :note    "A sample note."})
          timer2 (factories/create-timer organization-id
                                         #:timers{:user-id user-id
                                                  :task-id task-id-1
                                                  :note    "A sample note."})]
      (timer/start! db/datasource
                    user-id
                    (:timers/id timer1))
      (timer/start! db/datasource
                    user-id
                    (:timers/id timer2))
      (is (not (timer/running? (timer/find-by-user-and-id db/datasource
                                                          user-id
                                                          (:timers/id timer1))))))))

(deftest start-timer-isolation-by-user-test
  (testing "Users trying to start each others' timers."
    (let [{:keys [organization-id admin-id user-id task-id-1]} (setup-org-users-tasks!)
          task-id task-id-1
          unstarted-timer-by-user-1 (factories/create-timer organization-id
                                                            #:timers{:user-id admin-id
                                                                     :task-id task-id
                                                                     :note "A note by user-1."})
          unstarted-timer-by-user-2 (factories/create-timer organization-id
                                                            #:timers{:user-id user-id
                                                                     :task-id task-id
                                                                     :note "A note by user-2."})]
      (is (nil? (timer/start! db/datasource
                              user-id
                              (:timers/id unstarted-timer-by-user-1)))
          "user-2 cannot start user-1's timer.")
      (is (nil? (timer/start! db/datasource
                              admin-id
                              (:timers/id unstarted-timer-by-user-2)))
          "user-1 cannot start user-2's timer."))))


;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Stop Timer Tests
;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(deftest stop-unstarted-timer-test
  (testing "stopping an unstarted timer (having no time spans)"
    (let [{:keys [user-id organization-id task-id-1]} (setup-org-users-tasks!)]
      (is (nil? (timer/stop! db/datasource
                             user-id
                             (:timers/id (factories/create-timer organization-id
                                                                 #:timers{:user-id user-id
                                                                          :task-id task-id-1
                                                                          :note "A sample note."}))))
          "does nothing. An un-started timer remains un-started."))))

(deftest stop-running-timer-test
  (testing "stopping a running timer (having at least one time span)"
    (let [{:keys [user-id organization-id task-id-1]} (setup-org-users-tasks!)
          timer (let [timer (factories/create-timer organization-id
                                                    #:timers{:user-id user-id
                                                             :task-id task-id-1
                                                             :note "A sample note."})]
                  (timer/start! db/datasource
                                user-id
                                (:timers/id timer))
                  timer)]
      (is (s/valid? :timers/timer
                    (timer/stop! db/datasource
                                 user-id
                                 (:timers/id timer)))
          "returns a stopped timer.")
      (is (nil? (timer/stop! db/datasource
                             user-id
                             (:timers/id timer)))
          "does nothing. A stopped timer remain stopped."))))

(deftest stop-timer-isolation-by-user-test
  (testing "Users trying to stop each others' timers."
    (let [{:keys [organization-id admin-id user-id task-id-1]} (setup-org-users-tasks!)
          running-timer-by-user-1 (let [timer (factories/create-timer organization-id
                                                                      #:timers{:user-id admin-id
                                                                               :task-id task-id-1
                                                                               :note "A note by user-1."})]
                                    (timer/start! db/datasource
                                                  admin-id
                                                  (:timers/id timer))
                                    timer)
          running-timer-by-user-2 (let [timer (factories/create-timer organization-id
                                                                      #:timers{:user-id user-id
                                                                               :task-id task-id-1
                                                                               :note "A note by user-1."})]
                                    (timer/start! db/datasource
                                                  user-id
                                                  (:timers/id timer))
                                    timer)]
      (is (nil? (timer/stop! db/datasource
                             user-id
                             (:timers/id running-timer-by-user-1)))
          "user-2 cannot stop user-1's timer.")
      (is (nil? (timer/stop! db/datasource
                             admin-id
                             (:timers/id running-timer-by-user-2)))
          "user-1 cannot start user-2's timer."))))


;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Find Timer Tests
;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(deftest find-unstarted-timer-by-timer-id
  (testing "when we find a timer by timer id, and it was never started"
    (let [{:keys [user-id organization-id task-id-1]} (setup-org-users-tasks!)
          {timer-id :timers/id} (factories/create-timer organization-id
                                                        #:timers{:user-id user-id
                                                                 :task-id task-id-1
                                                                 :note "A sample note."})
          timer (timer/find-by-user-and-id db/datasource
                                           user-id
                                           timer-id)]
      (is (s/valid? :timers/timer
                    timer)
          "we get back a valid Timer object."))))

(deftest find-started-timer-by-timer-id
  (testing "when we find a timer by timer id, and it was started and/or stopped at least once"
    (with-redefs [time/now (constantly (time/now))]
      (let [{:keys [organization-id user-id task-id-1]} (setup-org-users-tasks!)
            {timer-id :timers/id} (factories/create-timer organization-id
                                                          #:timers{:user-id user-id
                                                                   :task-id task-id-1
                                                                   :note "A sample note."})
            final-timer (last
                         (repeatedly 3
                                     (fn []
                                       (timer/start! db/datasource
                                                     user-id
                                                     timer-id)
                                       (timer/stop! db/datasource
                                                    user-id
                                                    timer-id))))]
        ;; validate timer data retrieved
        (is (s/valid? :timers/timer
                      (timer/find-by-user-and-id db/datasource
                                                 user-id
                                                 timer-id))
            "we get back a valid Timer object.")
        (is (= final-timer
               (timer/find-by-user-and-id db/datasource
                                          user-id
                                          timer-id))
            "The object contains Timer data.")))))

(deftest find-by-user-and-task-test
  (testing "when we find timers for a user's task"
    (let [{:keys [organization-id user-id task-id-1]} (setup-org-users-tasks!)]
      (dotimes [_ 3]
        (->> (factories/create-timer organization-id
                                     #:timers{:user-id user-id
                                              :task-id task-id-1
                                              :note "A sample note."})
             :timers/id
             (timer/start! db/datasource user-id)
             :time-spans/timer-id
             (timer/stop! db/datasource user-id)
             :time-spans/timer-id
             (timer/start! db/datasource user-id)))

      (is (= 3 (count (timer/find-by-user-and-task db/datasource
                                                   user-id
                                                   task-id-1)))
          "we find all the associated Timer objects")
      (is (s/valid? (s/coll-of :timers/timer)
                    (timer/find-by-user-and-task db/datasource
                                                 user-id
                                                 task-id-1))
          "each Timer object has Timer information"))))

(deftest find-timer-isolation-by-user-test
  (testing "Users trying to read each others' timers."
    (let [{:keys [organization-id admin-id user-id task-id-1 task-id-2]} (setup-org-users-tasks!)
          task-id-timed-by-user-1 task-id-1
          task-id-timed-by-user-2 task-id-2
          timer-for-task-timed-by-user-1 (factories/create-timer organization-id
                                                                 #:timers{:user-id admin-id
                                                                          :task-id task-id-timed-by-user-1
                                                                          :note "A note by user-1."})
          timer-for-task-timed-by-user-2 (factories/create-timer organization-id
                                                                 #:timers{:user-id user-id
                                                                          :task-id task-id-timed-by-user-2
                                                                          :note "A note by user-2."})]
      (is (nil? (timer/find-by-user-and-id db/datasource
                                           user-id
                                           (:timers/id timer-for-task-timed-by-user-1)))
          "user-2 cannot fetch user-1's timers by timer id.")
      (is (nil? (timer/find-by-user-and-id db/datasource
                                           admin-id
                                           (:timers/id timer-for-task-timed-by-user-2)))
          "user-1 cannot fetch user-2's timer by timer id.")
      (is (nil? (timer/find-by-user-and-task db/datasource
                                             user-id
                                             task-id-timed-by-user-1))
          "user-2 cannot fetch user-1's timers by task id.")
      (is (nil? (timer/find-by-user-and-task db/datasource
                                             admin-id
                                             task-id-timed-by-user-2))
          "user-1 cannot fetch user-2's timers by task id."))))

(deftest find-by-user-and-recorded-for-test
  (tu/with-fixtures [fixtures/clear-db]
    (testing "Returns timers when timers exist for the user on the given date"
      (let [{:keys [organization-id task-id-1 task-id-2 task-1 task-2 user-id]} (setup-org-users-tasks!)
            recorded-date (LocalDate/parse "2020-01-14")
            timer-1 (factories/create-timer organization-id #:timers{:user-id      user-id
                                                                     :task-id      task-id-1
                                                                     :note         "note1"
                                                                     :recorded-for recorded-date})
            timer-2 (factories/create-timer organization-id #:timers{:user-id      user-id
                                                                     :task-id      task-id-2
                                                                     :note         "note2"
                                                                     :recorded-for recorded-date})]
        (is (= [(assoc timer-1 :task task-1)
                (assoc timer-2 :task task-2)]
               (timer/find-by-user-and-recorded-for db/datasource
                                                    user-id
                                                    recorded-date))))))
  (testing "Returns an empty vector when timers don't exist for the user on the given date"
    (let [{:keys [admin-id organization-id task-id-1 task-id-2 user-id]} (setup-org-users-tasks!)
          _timer-1 (factories/create-timer organization-id #:timers{:user-id      admin-id
                                                                    :task-id      task-id-1
                                                                    :note         "note1"
                                                                    :recorded-for (LocalDate/parse "2020-01-13")})
          _timer-2 (factories/create-timer organization-id #:timers{:user-id      user-id
                                                                    :task-id      task-id-2
                                                                    :note         "note2"
                                                                    :recorded-for (LocalDate/parse "2020-01-14")})]
      (is (= []
             (timer/find-by-user-and-recorded-for db/datasource admin-id (LocalDate/parse "2020-01-14")))))))
