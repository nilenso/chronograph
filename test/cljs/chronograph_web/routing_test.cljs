(ns chronograph-web.routing-test
  (:require [cljs.test :refer-macros [deftest is run-tests use-fixtures]]
            [re-frame.core :as rf]
            [re-frame.db]
            [chronograph-web.events.routing :as routing-events]
            [chronograph-web.subscriptions :as subs]
            [chronograph-web.fixtures :as fixtures]
            [chronograph-web.test-utils :as tu]))

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
  (tu/rf-test "when page is set"
    (def-test-handlers)
    (rf/dispatch [::routing-events/pushy-dispatch {:handler ::test}])
    (is (= {:handler ::test}
           @(rf/subscribe [::subs/current-page]))
        "the current page should be set")
    (is (= (:pre-change @re-frame.db/app-db) :handled) "the pre-change handler should run")
    (is (= (:change @re-frame.db/app-db) :handled) "the change handler should run")))
