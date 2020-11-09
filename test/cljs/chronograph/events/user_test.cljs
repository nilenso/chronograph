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
     (rf/reg-fx :http-xhrio
       (fn [{:keys [uri on-success]}]
         (case uri
           "/api/users/me" (rf/dispatch (conj on-success {:id        123
                                                          :name      "Foo Bar"
                                                          :email     "foo@bar.baz"
                                                          :photo-url "https://foo.png"}))
           "/api/organizations/" (rf/dispatch (conj on-success [{:id   42
                                                                 :name "A Test Org"
                                                                 :slug "test-1"
                                                                 :role "member"}
                                                                {:id   43
                                                                 :name "A Test Org 2"
                                                                 :slug "test-2"
                                                                 :role "admin"}]))
           nil)))
     (rf/dispatch [::user-events/initialize])
     (is (= {:signin-state :signed-in
             :id           123
             :name         "Foo Bar"
             :email        "foo@bar.baz"
             :photo-url    "https://foo.png"}
            @(rf/subscribe [::subs/user-info]))
         "the user info should be in the DB and the user should be signed in")

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

  (testing "when the profile fetch fails"
    (rf-test/run-test-sync
     (rf/reg-fx :http-xhrio
       (fn [_]
         (rf/dispatch [::user-events/fetch-data-failed])))
     (rf/dispatch [::user-events/initialize])
     (is (= :signed-out
            @(rf/subscribe [::subs/signin-state]))
         "the user should be considered signed out"))))

