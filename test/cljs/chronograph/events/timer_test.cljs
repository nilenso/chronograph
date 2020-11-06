(ns chronograph.events.timer-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests use-fixtures]]
            [chronograph.fixtures :as fixtures]
            [day8.re-frame.test :as rf-test]
            [chronograph.test-utils :as tu]
            [re-frame.core :as rf]
            [clojure.test.check.generators]
            [chronograph.specs]
            [chronograph-web.subscriptions :as subs]
            [chronograph-web.events.timer :as timer-events]
            [chronograph-web.utils.time :as time]
            [medley.core :as medley]))

(use-fixtures :once fixtures/silence-logging fixtures/check-specs)

(def timers-response {:timers [{:id           "ab289769-609d-46ce-a6cf-601bf716fcf6"
                                :task-id      2
                                :user-id      1
                                :recorded-for "2020-03-14"
                                :note         ""
                                :time-spans   [{:started-at "2020-11-05T11:08:24.500944Z"
                                                :stopped-at "2020-11-05T12:08:24.501902Z"}
                                               {:started-at "2020-11-05T13:08:24.500944Z"
                                                :stopped-at "2020-11-05T14:08:24.501902Z"}]
                                :task         {:id              2
                                               :name            "baz"
                                               :description     "quux"
                                               :organization-id 3}}
                               {:id           "43fea53d-56bb-4e40-9402-e8164613084d"
                                :task-id      3
                                :user-id      1
                                :recorded-for "2020-03-14"
                                :note         "baz"
                                :time-spans   [{:started-at "2020-11-05T15:08:24.500944Z"
                                                :stopped-at "2020-11-05T16:08:24.501902Z"}
                                               {:started-at "2020-11-05T17:08:24.500944Z"
                                                :stopped-at "2020-11-05T18:08:24.501902Z"}]
                                :task         {:id              3
                                               :name            "quuxy"
                                               :description     "quux"
                                               :organization-id 3}}]})

(def expected-timers [{:id           #uuid"ab289769-609d-46ce-a6cf-601bf716fcf6"
                       :task-id      2
                       :user-id      1
                       :recorded-for {:day   14
                                      :month 2
                                      :year  2020}
                       :note         ""
                       :time-spans   (->> [{:started-at "2020-11-05T11:08:24.500944Z"
                                            :stopped-at "2020-11-05T12:08:24.501902Z"}
                                           {:started-at "2020-11-05T13:08:24.500944Z"
                                            :stopped-at "2020-11-05T14:08:24.501902Z"}]
                                          (map (partial medley/map-vals time/string->date)))
                       :task         {:id              2
                                      :name            "baz"
                                      :description     "quux"
                                      :organization-id 3}}
                      {:id           #uuid"43fea53d-56bb-4e40-9402-e8164613084d"
                       :task-id      3
                       :user-id      1
                       :recorded-for {:day   14
                                      :month 2
                                      :year  2020}
                       :note         "baz"
                       :time-spans   (->> [{:started-at "2020-11-05T15:08:24.500944Z"
                                            :stopped-at "2020-11-05T16:08:24.501902Z"}
                                           {:started-at "2020-11-05T17:08:24.500944Z"
                                            :stopped-at "2020-11-05T18:08:24.501902Z"}]
                                          (map (partial medley/map-vals time/string->date)))
                       :task         {:id              3
                                      :name            "quuxy"
                                      :description     "quux"
                                      :organization-id 3}}])

(deftest fetch-timers-test
  (testing "When timers are successfully fetched, the db is updated"
    (rf-test/run-test-sync
     (tu/initialize-db!)
     (tu/stub-xhrio timers-response true)
     (rf/dispatch [::timer-events/fetch-timers "2020-03-14"])
     (is (= expected-timers @(rf/subscribe [::subs/timers {:day 14 :month 2 :year 2020} 3])))))

  (testing "When fetching timers fails, there is no change"
    (rf-test/run-test-sync
     (tu/initialize-db!)
     (tu/stub-xhrio {} false)
     (let [error-params (tu/stub-effect :flash-error)]
       (rf/dispatch [::timer-events/fetch-timers "2020-03-14"])
       (is (some? @error-params)
           "An error message should be flashed.")))))