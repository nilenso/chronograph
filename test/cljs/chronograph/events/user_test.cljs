(ns chronograph.events.user-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests use-fixtures]]
            [day8.re-frame.test :as rf-test]
            [re-frame.core :as rf]
            [chronograph-web.events.user :as user-events]
            [chronograph-web.subscriptions :as subs]
            [chronograph.fixtures :as fixtures]
            [chronograph.test-utils :as tu]))

(use-fixtures :once fixtures/silence-logging fixtures/check-specs)

(deftest initialize-test
  (testing "when the profile is fetched successfully"
    (rf-test/run-test-sync
     (tu/stub-routing)
     (tu/stub-xhrio {:id        123
                     :name      "Foo Bar"
                     :email     "foo@bar.baz"
                     :photo-url "https://foo.png"}
                    true)

     (rf/dispatch [::user-events/initialize])

     (is (= {:signin-state :signed-in
             :id           123
             :name         "Foo Bar"
             :email        "foo@bar.baz"
             :photo-url    "https://foo.png"}
            @(rf/subscribe [::subs/user-info]))
         "the user info should be in the DB and the user should be signed in")))

  (testing "when the profile fetch fails"
    (rf-test/run-test-sync
     (tu/stub-xhrio {} false)

     (rf/dispatch [::user-events/initialize])

     (is (= :signed-out
            @(rf/subscribe [::subs/signin-state]))
         "the user should be considered signed out"))))

