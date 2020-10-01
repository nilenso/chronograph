(ns chronograph.pages.pending-invites.events-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests use-fixtures]]
            [chronograph.fixtures :as fixtures]
            [day8.re-frame.test :as rf-test]
            [chronograph.test-utils :as tu]
            [re-frame.core :as rf]
            [re-frame.db :as db]
            [chronograph-web.pages.pending-invites.events :as invites]
            [chronograph-web.pages.pending-invites.subscriptions :as invites-subs]
            [chronograph-web.subscriptions :as subs]
            [chronograph-web.effects]))

(use-fixtures :once fixtures/check-specs)

(deftest pending-invites-page-test
  (testing "When page is mounted, db should contain invites"
    (rf-test/run-test-sync
     (tu/initialize-db!)
     (tu/set-token "/pending-invites")
     (rf/dispatch [::invites/page-mounted])
     (is (= [{:id 1 :slug "slug1" :name "org1"}
             {:id 2 :slug "slug2" :name "org2"}
             {:id 3 :slug "slug3" :name "org3"}]
            @(rf/subscribe [::invites-subs/invites])))))
  (testing "When invite is rejected, it should be removed from db"
    (rf-test/run-test-sync
     (tu/initialize-db!)
     (swap! db/app-db assoc :organization-invites {1 {:id 1 :slug "slug1" :name "org1"}
                                                   2 {:id 2 :slug "slug2" :name "org2"}
                                                   3 {:id 3 :slug "slug3" :name "org3"}})
     (rf/dispatch [::invites/reject-invite 2])
     (is (= [{:id 1 :slug "slug1" :name "org1"}
             {:id 3 :slug "slug3" :name "org3"}]
            @(rf/subscribe [::invites-subs/invites])))))

  (testing "When invite is accepted, it should be removed from db and user is sent to organization detail page"
    (rf-test/run-test-sync
     (tu/stub-routing)
     (tu/initialize-db!)
     (swap! re-frame.db/app-db assoc :organization-invites {1 {:id 1 :slug "slug1" :name "org1"}
                                                            2 {:id 2 :slug "slug2" :name "org2"}
                                                            3 {:id 3 :slug "slug3" :name "org3"}})
     (rf/dispatch [::invites/accept-invite 2])
     (is (= [{:id 1 :slug "slug1" :name "org1"}
             {:id 3 :slug "slug3" :name "org3"}]
            @(rf/subscribe [::invites-subs/invites])))
     (is (= {:route-params {:slug "slug2"}, :handler :organization-show}
            @(rf/subscribe [::subs/current-page]))))))