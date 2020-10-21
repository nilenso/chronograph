(ns chronograph.pages.landing.events-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests use-fixtures]]
            [chronograph.fixtures :as fixtures]
            [day8.re-frame.test :as rf-test]
            [chronograph.test-utils :as tu]
            [re-frame.core :as rf]
            [re-frame.db :as db]
            [chronograph-web.pages.landing.events :as landing-events]
            [chronograph-web.pages.landing.subscriptions :as landing-subs]
            [chronograph-web.subscriptions :as subs]
            [chronograph-web.effects]))

(use-fixtures :once fixtures/check-specs)

(deftest landing-page-test
  (testing "When invited orgs are fetched, db should contain them"
    (let [invited-orgs [{:id 1 :slug "slug1" :name "org1"}
                        {:id 2 :slug "slug2" :name "org2"}
                        {:id 3 :slug "slug3" :name "org3"}]]
      (rf-test/run-test-sync
       (tu/initialize-db!)
       (rf/reg-fx :http-xhrio
         (fn [_]
           (rf/dispatch [::landing-events/fetch-invited-orgs-success
                         invited-orgs])))
       (rf/dispatch [::landing-events/fetch-invited-orgs])
       (is (= invited-orgs
              @(rf/subscribe [::landing-subs/invites]))))))

  (testing "When invite is rejected, it should be removed from db"
    (rf-test/run-test-sync
     (tu/initialize-db!)
     (swap! db/app-db assoc :organization-invites {1 {:id 1 :slug "slug1" :name "org1"}
                                                   2 {:id 2 :slug "slug2" :name "org2"}
                                                   3 {:id 3 :slug "slug3" :name "org3"}})
     (rf/reg-fx :http-xhrio
       (fn [_]
         (rf/dispatch [::landing-events/reject-invite-succeeded 2])))
     (rf/dispatch [::landing-events/reject-invite 2])
     (is (= [{:id 1 :slug "slug1" :name "org1"}
             {:id 3 :slug "slug3" :name "org3"}]
            @(rf/subscribe [::landing-subs/invites])))))

  (testing "When invite is accepted"
    (rf-test/run-test-sync
     (tu/stub-routing)
     (tu/initialize-db!)
     (swap! re-frame.db/app-db assoc :organization-invites {1 {:id 1 :slug "slug1" :name "org1"}
                                                            2 {:id 2 :slug "slug2" :name "org2"}
                                                            3 {:id 3 :slug "slug3" :name "org3"}})
     (rf/reg-fx :http-xhrio
       (fn [_]
         (rf/dispatch [::landing-events/accept-invite-succeeded 2])))
     (rf/dispatch [::landing-events/accept-invite 2])
     (is (= [{:id 1 :slug "slug1" :name "org1"}
             {:id 3 :slug "slug3" :name "org3"}]
            @(rf/subscribe [::landing-subs/invites]))
         "it should be removed from the pending invites map in the app db.")
     (is (= {:id 2 :slug "slug2" :name "org2"}
            @(rf/subscribe [::subs/organization "slug2"]))
         "it should be added to the organizations map in the app db.")
     (is (= {:handler :root}
            @(rf/subscribe [::subs/current-page]))
         "the user remains on the landing page."))))
