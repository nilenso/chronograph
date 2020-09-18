(ns chronograph.routing-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests]]
            [day8.re-frame.test :as rf-test]
            [re-frame.core :as rf]
            [chronograph-web.events.routing :as routing-events]
            [chronograph-web.subscriptions :as subs]))

(deftest pushy-dispatch-test
  (testing "when page is set"
    (rf-test/run-test-sync
     (rf/dispatch [::routing-events/pushy-dispatch :test])
     (is (= :test
            @(rf/subscribe [::subs/current-page]))
         "the current page should be set"))))
