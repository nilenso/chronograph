(ns chronograph.core-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests]]
            [day8.re-frame.test :as rf-test]
            [re-frame.core :as rf]
            [chronograph-web.events :as events]
            [chronograph-web.subscriptions :as subs]))

(deftest initialize-test
  (testing "when the profile is fetched successfully"
    (rf-test/run-test-sync
      (rf/reg-fx :http-xhrio
                 (fn [_]
                   (rf/dispatch [::events/fetch-profile-succeeded {:id        123
                                                                   :name      "Foo Bar"
                                                                   :email     "foo@bar.baz"
                                                                   :photo-url "https://foo.png"}])))
      (rf/dispatch [::events/initialize])
      (is (= {:signin-state :signed-in
              :id           123
              :name         "Foo Bar"
              :email        "foo@bar.baz"
              :photo-url    "https://foo.png"}
             @(rf/subscribe [::subs/user-info]))
          "the user info should be in the DB and the user should be signed in")))

  (testing "when the profile fetch fails"
    (rf-test/run-test-sync
      (rf/reg-fx :http-xhrio
                 (fn [_]
                   (rf/dispatch [::events/fetch-profile-failed "dummy"])))
      (rf/dispatch [::events/initialize])
      (is (= :signed-out
             @(rf/subscribe [::subs/signin-state]))
          "the user should be considered signed out"))))
