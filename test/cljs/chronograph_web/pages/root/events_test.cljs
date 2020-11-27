(ns chronograph-web.pages.root.events-test
  (:require [cljs.test :refer-macros [deftest is run-tests use-fixtures]]
            [re-frame.core :as rf]
            [chronograph-web.subscriptions :as subs]
            [chronograph-web.fixtures :as fixtures]
            [chronograph-web.test-utils :as tu]
            [chronograph-web.pages.root.events :as root-events]
            [chronograph-web.pages.timers.events :as timers-page-events]))

(use-fixtures :once fixtures/silence-logging fixtures/check-specs)

(deftest root-page-navigated-test
  (tu/rf-test "when the root page is navigated to"
    (tu/stub-xhrio [{:id   42
                     :name "A Test Org"
                     :slug "test-1"
                     :role "member"}
                    {:id   43
                     :name "A Test Org 2"
                     :slug "test-2"
                     :role "admin"}]
                   true)
    (tu/stub-event ::timers-page-events/timers-page-navigated)

    (rf/dispatch [::root-events/root-page-navigated])

    (is (= :root @(rf/subscribe [::subs/page-key])) "the page-key should be set")

    (is (= [{:id   42
             :name "A Test Org"
             :slug "test-1"
             :role "member"}
            {:id   43
             :name "A Test Org 2"
             :slug "test-2"
             :role "admin"}]
           @(rf/subscribe [::subs/organizations]))
        "the organizations should be in the DB")

    (is (= {:handler      :timers-list
            :route-params {:slug "test-1"}}
           @(rf/subscribe [::subs/current-page]))
        "the route should be set to the overview of the first org in the returned list")))
