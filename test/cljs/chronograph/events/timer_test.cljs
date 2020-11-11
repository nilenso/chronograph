(ns chronograph.events.timer-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests use-fixtures]]
            [chronograph.fixtures :as fixtures]
            [day8.re-frame.test :as rf-test]
            [chronograph.test-utils :as tu]
            [re-frame.db]
            [re-frame.core :as rf]
            [clojure.test.check.generators]
            [chronograph.specs]
            [chronograph-web.pages.timers.subscriptions :as timers-subs]
            [chronograph-web.events.timer :as timer-events]
            [chronograph-web.utils.time :as time]
            [medley.core :as medley]
            [chronograph-web.routes :as routes]
            [chronograph-web.db :as db]
            [chronograph-web.db.organization :as org-db]))

(use-fixtures :once fixtures/silence-logging fixtures/check-specs)

(def timers-response {:timers [{:id           "ab289769-609d-46ce-a6cf-601bf716fcf6"
                                :task-id      2
                                :user-id      1
                                :recorded-for "2020-03-14"
                                :note         ""
                                :created-at   "2020-11-05T11:08:24.500944Z"
                                :updated-at   "2020-11-05T11:08:24.500944Z"
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
                                :created-at   "2020-11-05T12:08:24.500944Z"
                                :updated-at   "2020-11-05T12:08:24.500944Z"
                                :time-spans   [{:started-at "2020-11-05T15:08:24.500944Z"
                                                :stopped-at "2020-11-05T16:08:24.501902Z"}
                                               {:started-at "2020-11-05T17:08:24.500944Z"
                                                :stopped-at "2020-11-05T18:08:24.501902Z"}]
                                :task         {:id              3
                                               :name            "quuxy"
                                               :description     "quux"
                                               :organization-id 3}}
                               {:id           "35947099-178e-490a-8fa3-9d37e26984bc"
                                :task-id      4
                                :user-id      1
                                :recorded-for "2020-03-14"
                                :note         "baz"
                                :created-at   "2020-13-05T13:08:24.500944Z"
                                :updated-at   "2020-13-05T13:08:24.500944Z"

                                :time-spans   [{:started-at "2020-11-05T19:08:24.500944Z"
                                                :stopped-at "2020-11-05T20:08:24.501902Z"}
                                               {:started-at "2020-11-05T21:08:24.500944Z"
                                                :stopped-at "2020-11-05T22:08:24.501902Z"}]
                                :task         {:id              4
                                               :name            "foobar"
                                               :description     "quux"
                                               :organization-id 4}}]})

(def expected-timers [{:id           #uuid"43fea53d-56bb-4e40-9402-e8164613084d"
                       :task-id      3
                       :user-id      1
                       :recorded-for {:day   14
                                      :month 2
                                      :year  2020}
                       :note         "baz"
                       :created-at   (time/string->date "2020-11-05T12:08:24.500944Z")
                       :updated-at   (time/string->date "2020-11-05T12:08:24.500944Z")
                       :time-spans   (->> [{:started-at "2020-11-05T15:08:24.500944Z"
                                            :stopped-at "2020-11-05T16:08:24.501902Z"}
                                           {:started-at "2020-11-05T17:08:24.500944Z"
                                            :stopped-at "2020-11-05T18:08:24.501902Z"}]
                                          (map (partial medley/map-vals time/string->date)))
                       :task         {:id              3
                                      :name            "quuxy"
                                      :description     "quux"
                                      :organization-id 3}}
                      {:id           #uuid"ab289769-609d-46ce-a6cf-601bf716fcf6"
                       :task-id      2
                       :user-id      1
                       :recorded-for {:day   14
                                      :month 2
                                      :year  2020}
                       :note         ""
                       :created-at   (time/string->date "2020-11-05T11:08:24.500944Z")
                       :updated-at   (time/string->date "2020-11-05T11:08:24.500944Z")
                       :time-spans   (->> [{:started-at "2020-11-05T11:08:24.500944Z"
                                            :stopped-at "2020-11-05T12:08:24.501902Z"}
                                           {:started-at "2020-11-05T13:08:24.500944Z"
                                            :stopped-at "2020-11-05T14:08:24.501902Z"}]
                                          (map (partial medley/map-vals time/string->date)))
                       :task         {:id              2
                                      :name            "baz"
                                      :description     "quux"
                                      :organization-id 3}}])

(deftest fetch-timers-test
  (testing "When timers are successfully fetched, the db is updated"
    (rf-test/run-test-sync
     (tu/initialize-db!)
     (tu/stub-routing)
     (tu/set-token (routes/path-for :timers-list :slug "test-slug"))
     (let [date {:day 14 :month 2 :year 2020}]
       (swap! re-frame.db/app-db org-db/add-org {:id   3
                                                 :slug "test-slug"
                                                 :name "test"
                                                 :role "member"})
       (swap! re-frame.db/app-db db/set-in-page-state [:selected-date] date)
       (tu/stub-xhrio timers-response true)
       (rf/dispatch [::timer-events/fetch-timers date])
       (is (= expected-timers
              @(rf/subscribe [::timers-subs/current-organization-timers]))))))

  (testing "When fetching timers fails, there is no change"
    (rf-test/run-test-sync
     (tu/initialize-db!)
     (tu/stub-xhrio {} false)
     (let [error-params (tu/stub-effect :flash-error)]
       (rf/dispatch [::timer-events/fetch-timers "2020-03-14"])
       (is (some? @error-params)
           "An error message should be flashed.")))))
