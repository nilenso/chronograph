(ns chronograph.pages.timers.events-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests use-fixtures]]
            [chronograph-web.events.organization-invites :as org-invites-events]
            [chronograph.fixtures :as fixtures]
            [day8.re-frame.test :as rf-test]
            [chronograph.test-utils :as tu]
            [chronograph.specs]
            [re-frame.core :as rf]
            [chronograph-web.pages.timers.events :as timers-events]
            [chronograph-web.events.timer :as timer-events]
            [chronograph-web.effects]
            [chronograph-web.utils.time :as time]))

(use-fixtures :once fixtures/silence-logging fixtures/check-specs)

(def fake-now (let [d (js/Date.)]
                (.setYear d 2020)
                (.setMonth d 10)
                (.setDate d 5)
                d))

(deftest landing-page-test
  (testing "When the user navigates to the page, timers & invites should be fetched"
    (rf-test/run-test-sync
      ;; TODO: extract test setup stuff
     (tu/initialize-db!)
     (with-redefs [time/now (constantly fake-now)]
       (let [invited-orgs-stub (tu/stub-event ::org-invites-events/fetch-invited-orgs)
             fetch-timers-stub (tu/stub-event ::timer-events/fetch-timers)]
         (rf/dispatch [::timers-events/timers-page-navigated])
         (is (= [::org-invites-events/fetch-invited-orgs] @invited-orgs-stub))
         (is (= [::timer-events/fetch-timers {:day 5 :month 10 :year 2020}] @fetch-timers-stub)))))))

(deftest timer-actions-test
  (testing "When starting a timer"
    (testing "succeeds, timers should be refetched"
      (rf-test/run-test-sync
       ;; TODO: extract test setup stuff
       (tu/initialize-db!)
       (with-redefs [time/now (constantly fake-now)]
         (let [fetch-timers-stub (tu/stub-event ::timer-events/fetch-timers)
               _                 (tu/stub-xhrio {} true)]
           (rf/dispatch [::timers-events/start-timer "foobar"])
           (is (= [::timer-events/fetch-timers {:day 5 :month 10 :year 2020} {}]
                  @fetch-timers-stub))))))

    (testing "fails, an error should be flashed"
      (rf-test/run-test-sync
       (tu/initialize-db!)
       (tu/stub-xhrio {} false)
       (let [error-params (tu/stub-effect :flash-error)]
         (rf/dispatch [::timers-events/start-timer "foobar"])
         (is (some? @error-params)
             "An error message should be flashed.")))))

  (testing "When stopping a timer"
    (testing "succeeds, timers should be refetched"
      (rf-test/run-test-sync
       ;; TODO: extract test setup stuff
       (tu/initialize-db!)
       (with-redefs [time/now (constantly fake-now)]
         (let [fetch-timers-stub (tu/stub-event ::timer-events/fetch-timers)
               _                 (tu/stub-xhrio {} true)]
           (rf/dispatch [::timers-events/stop-timer "foobar"])
           (is (= [::timer-events/fetch-timers {:day 5 :month 10 :year 2020} {}]
                  @fetch-timers-stub))))))

    (testing "fails, an error should be flashed"
      (rf-test/run-test-sync
       (tu/initialize-db!)
       (tu/stub-xhrio {} false)
       (let [error-params (tu/stub-effect :flash-error)]
         (rf/dispatch [::timers-events/stop-timer "foobar"])
         (is (some? @error-params)
             "An error message should be flashed.")))))

  (testing "When deleting a timer"
    (testing "succeeds, timers should be refetched"
      (rf-test/run-test-sync
       ;; TODO: extract test setup stuff
       (tu/initialize-db!)
       (with-redefs [time/now (constantly fake-now)]
         (let [fetch-timers-stub (tu/stub-event ::timer-events/fetch-timers)
               _                 (tu/stub-xhrio {} true)]
           (rf/dispatch [::timers-events/delete-timer "foobar"])
           (is (= [::timer-events/fetch-timers {:day 5 :month 10 :year 2020} {}]
                  @fetch-timers-stub))))))

    (testing "fails, an error should be flashed"
      (rf-test/run-test-sync
       (tu/initialize-db!)
       (tu/stub-xhrio {} false)
       (let [error-params (tu/stub-effect :flash-error)]
         (rf/dispatch [::timers-events/delete-timer "foobar"])
         (is (some? @error-params)
             "An error message should be flashed.")))))

  (testing "When creating a timer"
    (testing "succeeds"
      (rf-test/run-test-sync
       ;; TODO: extract test setup stuff
       (tu/initialize-db!)
       (with-redefs [time/now (constantly fake-now)]
         (let [fetch-timers-stub (tu/stub-event ::timer-events/fetch-timers)
               dismiss-stub      (tu/stub-event ::timers-events/dismiss-create-timer-widget)]
           (rf/dispatch [::timers-events/create-timer-succeeded])
           (is (= [::timer-events/fetch-timers {:day 5 :month 10 :year 2020}]
                  @fetch-timers-stub)
               "timers should be refetched")
           (is (= [::timers-events/dismiss-create-timer-widget]
                  @dismiss-stub)
               "the create timer widget should be dismissed")))))

    (testing "fails"
      (rf-test/run-test-sync
       (tu/initialize-db!)
       (tu/stub-xhrio {} false)
       (let [error-params (tu/stub-effect :flash-error)]
         (rf/dispatch [::timers-events/create-timer-failed])
         (is (some? @error-params)
             "An error message should be flashed."))))))
