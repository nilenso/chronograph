(ns chronograph-web.events.user-test
  (:require [cljs.test :refer-macros [deftest is run-tests use-fixtures]]
            [re-frame.core :as rf]
            [chronograph-web.events.user :as user-events]
            [chronograph-web.subscriptions :as subs]
            [chronograph-web.fixtures :as fixtures]
            [chronograph-web.test-utils :as tu]))

(use-fixtures :once fixtures/silence-logging fixtures/check-specs)

(deftest initialize-test
  (tu/rf-test "when the profile is fetched successfully"
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
        "the user info should be in the DB and the user should be signed in"))

  (tu/rf-test "when the profile fetch fails"
    (tu/stub-xhrio {} false)

    (rf/dispatch [::user-events/initialize])

    (is (= :signed-out
           @(rf/subscribe [::subs/signin-state]))
        "the user should be considered signed out")))

