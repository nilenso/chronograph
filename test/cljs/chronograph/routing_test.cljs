(ns chronograph.routing-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests]]
            [day8.re-frame.test :as rf-test]
            [re-frame.core :as rf]
            [chronograph-web.events.routing :as routing-events]
            [chronograph-web.subscriptions :as subs]))

(deftest set-token-test
  (testing "when history token is set"
    (rf-test/run-test-sync
     (let [actual (atom nil)]
       (rf/reg-fx :history-token #(reset! actual %))
       (rf/dispatch-sync [::routing-events/set-token "/test"])
       (is (= @actual "/test")
           "the history-token effect is effected")))))

(deftest set-page-test
  (testing "when page is set"
    (rf-test/run-test-sync
     (rf/dispatch-sync [::routing-events/set-page :test])
     (is (= :test
            @(rf/subscribe [::subs/current-page]))
         "the current page should be set"))))
