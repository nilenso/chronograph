(ns chronograph.routing-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests use-fixtures]]
            [day8.re-frame.test :as rf-test]
            [re-frame.core :as rf]
            [chronograph-web.events.routing :as routing-events]
            [chronograph-web.subscriptions :as subs]
            [chronograph.fixtures :as fixtures]
            [chronograph.test-utils :as tu]))

(use-fixtures :once fixtures/silence-logging fixtures/check-specs)

(deftest pushy-dispatch-test
  (testing "when page is set"
    (rf-test/run-test-sync
     (tu/initialize-db!)
     (rf/dispatch [::routing-events/pushy-dispatch {:handler :test}])
     (is (= {:handler :test}
            @(rf/subscribe [::subs/current-page]))
         "the current page should be set"))))
