(ns chronograph.routing-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests use-fixtures]]
            [day8.re-frame.test :as rf-test]
            [re-frame.core :as rf]
            [re-frame.db]
            [chronograph-web.events.routing :as routing-events]
            [chronograph-web.subscriptions :as subs]
            [chronograph.fixtures :as fixtures]
            [chronograph.test-utils :as tu]))

(use-fixtures :once fixtures/silence-logging fixtures/check-specs)

(defn def-test-handlers []
  (defmethod routing-events/on-pre-route-change-event
    ::test
    [_route {:keys [db]}]
    {:db (assoc db :pre-change :handled)})

  (rf/reg-event-fx
    ::test-change
    (fn [{:keys [db]} [_]]
      {:db (assoc db :change :handled)}))

  (defmethod routing-events/on-route-change-event
    ::test
    [_]
    [::test-change]))

(deftest pushy-dispatch-test
  (testing "when page is set"
    (def-test-handlers)
    (rf-test/run-test-sync
     (tu/initialize-db!)
     (rf/dispatch [::routing-events/pushy-dispatch {:handler ::test}])
     (is (= {:handler ::test}
            @(rf/subscribe [::subs/current-page]))
         "the current page should be set")
     (is (= (:pre-change @re-frame.db/app-db) :handled) "the pre-change handler should run")
     (is (= (:change @re-frame.db/app-db) :handled) "the change handler should run"))))
