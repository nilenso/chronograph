(ns chronograph-web.pages.timers.events-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests use-fixtures]]
            [chronograph-web.events.organization-invites :as org-invites-events]
            [chronograph-web.fixtures :as fixtures]
            [chronograph-web.test-utils :as tu]
            [chronograph.specs]
            [re-frame.core :as rf]
            [re-frame.db]
            [chronograph-web.pages.timers.events :as timers-events]
            [chronograph-web.pages.timers.subscriptions :as timers-subs]
            [chronograph-web.events.timer :as timer-events]
            [chronograph-web.events.routing :as routing-events]
            [chronograph-web.subscriptions :as subs]
            [chronograph-web.db.organization :as org-db]
            [chronograph-web.effects]
            [chronograph-web.utils.time :as time]
            [chronograph-web.db :as db]))

(use-fixtures :once fixtures/silence-logging fixtures/check-specs)

(def fake-now (let [d (js/Date.)]
                (.setYear d 2020)
                (.setMonth d 10)
                (.setDate d 5)
                d))

(deftest landing-page-test
  (tu/rf-test "When the user navigates to the page, timers & invites should be fetched"
    (with-redefs [time/now (constantly fake-now)]
      (let [invited-orgs-stub (tu/stub-event ::org-invites-events/fetch-invited-orgs)
            fetch-timers-stub (tu/stub-event ::timer-events/fetch-timers)]
        (rf/dispatch [::timers-events/timers-page-navigated (time/js-date->calendar-date (time/now))])
        (is (= :timers-list @(rf/subscribe [::subs/page-key])) "the page-key should be set")
        (is (= [::org-invites-events/fetch-invited-orgs] @invited-orgs-stub))
        (is (= [::timer-events/fetch-timers {:day 5 :month 10 :year 2020}] @fetch-timers-stub))))))

(deftest timer-actions-test
  (testing "When starting a timer"
    (tu/rf-test "succeeds, timers should be refetched"
      (with-redefs [time/now (constantly fake-now)]
        (let [fetch-timers-stub (tu/stub-event ::timer-events/fetch-timers)
              _                 (tu/stub-xhrio {} true)]
          (swap! re-frame.db/app-db db/set-in-page-state [:selected-date] {:day 5 :month 10 :year 2020})
          (rf/dispatch [::timers-events/start-timer "foobar"])
          (is (= [::timer-events/fetch-timers {:day 5 :month 10 :year 2020} {}]
                 @fetch-timers-stub)))))

    (tu/rf-test "fails, an error should be flashed"
      (tu/stub-xhrio {} false)
      (let [error-params (tu/stub-effect :flash-error)]
        (rf/dispatch [::timers-events/start-timer "foobar"])
        (is (some? @error-params)
            "An error message should be flashed."))))

  (testing "When stopping a timer"
    (tu/rf-test "succeeds, timers should be refetched"
      (with-redefs [time/now (constantly fake-now)]
        (let [fetch-timers-stub (tu/stub-event ::timer-events/fetch-timers)
              _                 (tu/stub-xhrio {} true)]
          (swap! re-frame.db/app-db db/set-in-page-state [:selected-date] {:day 5 :month 10 :year 2020})
          (rf/dispatch [::timers-events/stop-timer "foobar"])
          (is (= [::timer-events/fetch-timers {:day 5 :month 10 :year 2020} {}]
                 @fetch-timers-stub)))))

    (tu/rf-test "fails, an error should be flashed"
      (tu/stub-xhrio {} false)
      (let [error-params (tu/stub-effect :flash-error)]
        (rf/dispatch [::timers-events/stop-timer "foobar"])
        (is (some? @error-params)
            "An error message should be flashed."))))

  (testing "When deleting a timer"
    (tu/rf-test "succeeds, timers should be refetched"
      (with-redefs [time/now (constantly fake-now)]
        (let [fetch-timers-stub (tu/stub-event ::timer-events/fetch-timers)
              _                 (tu/stub-xhrio {} true)]
          (swap! re-frame.db/app-db db/set-in-page-state [:selected-date] {:day 5 :month 10 :year 2020})
          (rf/dispatch [::timers-events/delete-timer "foobar"])
          (is (= [::timer-events/fetch-timers {:day 5 :month 10 :year 2020} {}]
                 @fetch-timers-stub)))))

    (tu/rf-test "fails, an error should be flashed"
      (tu/stub-xhrio {} false)
      (let [error-params (tu/stub-effect :flash-error)]
        (rf/dispatch [::timers-events/delete-timer "foobar"])
        (is (some? @error-params)
            "An error message should be flashed."))))

  (testing "When creating a timer"
    (tu/rf-test "succeeds"
      (with-redefs [time/now (constantly fake-now)]
        (let [fetch-timers-stub (tu/stub-event ::timer-events/fetch-timers)
              dismiss-stub      (tu/stub-event ::timers-events/dismiss-create-timer-widget)]
          (swap! re-frame.db/app-db db/set-in-page-state [:selected-date] {:day 5 :month 10 :year 2020})
          (rf/dispatch [::timers-events/create-timer-succeeded])
          (is (= [::timer-events/fetch-timers {:day 5 :month 10 :year 2020}]
                 @fetch-timers-stub)
              "timers should be refetched")
          (is (= [::timers-events/dismiss-create-timer-widget]
                 @dismiss-stub)
              "the create timer widget should be dismissed"))))

    (tu/rf-test "fails"
      (tu/stub-xhrio {} false)
      (let [error-params (tu/stub-effect :flash-error)]
        (rf/dispatch [::timers-events/create-timer-failed])
        (is (some? @error-params)
            "An error message should be flashed.")))))

(defn add-org [id slug]
  (swap! re-frame.db/app-db org-db/add-org {:id   id
                                   :name "A Test Org"
                                   :slug slug
                                   :role "admin"}))

(deftest date-selection-test
  (tu/rf-test "when a date is picked from the calendar"
    (with-redefs [time/now (constantly fake-now)]
      (let [invited-orgs-stub (tu/stub-events ::org-invites-events/fetch-invited-orgs)
            fetch-timers-stub (tu/stub-events ::timer-events/fetch-timers)]
        (add-org 1 "test-slug")
        (rf/dispatch [::routing-events/pushy-dispatch {:handler      :timers-list
                                                       :route-params {:slug "test-slug"}}])
        (rf/dispatch [::timers-events/calendar-select-date {:day 5 :month 10 :year 2020}])

        (is (= [[::org-invites-events/fetch-invited-orgs]
                [::org-invites-events/fetch-invited-orgs]] @invited-orgs-stub)
            "the organization invites should be re-fetched")
        (is (= [[::timer-events/fetch-timers {:day 5 :month 10 :year 2020}]
                [::timer-events/fetch-timers {:day 5 :month 10 :year 2020}]] @fetch-timers-stub)
            "the timers should be re-fetched")
        (is (= @(rf/subscribe [::timers-subs/selected-date]) {:day 5 :month 10 :year 2020})
            "the selected-date should be updated"))))

  (tu/rf-test "when the selected date is incremented"
    (with-redefs [time/now (constantly fake-now)]
      (let [invited-orgs-stub (tu/stub-events ::org-invites-events/fetch-invited-orgs)
            fetch-timers-stub (tu/stub-events ::timer-events/fetch-timers)]
        (add-org 1 "test-slug")
        (rf/dispatch [::routing-events/pushy-dispatch {:handler      :timers-list
                                                       :route-params {:slug "test-slug"}}])
        (rf/dispatch [::timers-events/modify-selected-date 1])
        (is (= [[::org-invites-events/fetch-invited-orgs]
                [::org-invites-events/fetch-invited-orgs]] @invited-orgs-stub)
            "the organization invites should be re-fetched")
        (is (= [[::timer-events/fetch-timers {:day 5 :month 10 :year 2020}]
                [::timer-events/fetch-timers {:day 6 :month 10 :year 2020}]] @fetch-timers-stub)
            "the timers should be re-fetched")
        (is (= @(rf/subscribe [::timers-subs/selected-date]) {:day 6 :month 10 :year 2020})
            "the selected-date should be increased by one day"))))

  (tu/rf-test "when the selected date is decremented"
    (with-redefs [time/now (constantly fake-now)]
      (let [invited-orgs-stub (tu/stub-events ::org-invites-events/fetch-invited-orgs)
            fetch-timers-stub (tu/stub-events ::timer-events/fetch-timers)]
        (add-org 1 "test-slug")
        (rf/dispatch [::routing-events/pushy-dispatch {:handler      :timers-list
                                                       :route-params {:slug "test-slug"}}])
        (rf/dispatch [::timers-events/modify-selected-date -1])
        (is (= [[::org-invites-events/fetch-invited-orgs]
                [::org-invites-events/fetch-invited-orgs]] @invited-orgs-stub)
            "the organization invites should be re-fetched")
        (is (= [[::timer-events/fetch-timers {:day 5 :month 10 :year 2020}]
                [::timer-events/fetch-timers {:day 4 :month 10 :year 2020}]] @fetch-timers-stub)
            "the timers should be re-fetched")
        (is (= @(rf/subscribe [::timers-subs/selected-date]) {:day 4 :month 10 :year 2020})
            "the selected-date should be reduced by one day")))))

