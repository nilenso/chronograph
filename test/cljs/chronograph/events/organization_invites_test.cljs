(ns chronograph.events.organization-invites-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests use-fixtures]]
            [chronograph-web.events.organization-invites :as org-invites-events]
            [chronograph-web.events.organization :as org-events]
            [chronograph.fixtures :as fixtures]
            [day8.re-frame.test :as rf-test]
            [chronograph.test-utils :as tu]
            [re-frame.core :as rf]
            [re-frame.db]
            [chronograph-web.db.organization-invites :as org-invites-db]))

(use-fixtures :once fixtures/silence-logging fixtures/check-specs)

(deftest fetch-invited-orgs-test
  (testing "When invited orgs are fetched, db should contain them"
    (let [invited-orgs [{:id 1 :slug "slug1" :name "org1"}
                        {:id 2 :slug "slug2" :name "org2"}
                        {:id 3 :slug "slug3" :name "org3"}]]
      (rf-test/run-test-sync
       (tu/initialize-db!)
       (tu/stub-xhrio invited-orgs true)
       (rf/dispatch [::org-invites-events/fetch-invited-orgs [::foobar]])
       (is (= invited-orgs
              (org-invites-db/invites @re-frame.db/app-db)))))))

(deftest reject-invite-test
  (testing "When invite is rejected, it should be removed from db"
    (rf-test/run-test-sync
     (tu/initialize-db!)
     (swap! re-frame.db/app-db assoc :organization-invites {1 {:id 1 :slug "slug1" :name "org1"}
                                                            2 {:id 2 :slug "slug2" :name "org2"}
                                                            3 {:id 3 :slug "slug3" :name "org3"}})
     (tu/stub-xhrio {} true)
     (let [post-success-event (tu/stub-event ::foobar)]
       (rf/dispatch [::org-invites-events/reject-invite "slug2" {:on-success [::foobar]}])
       (is (= [{:id 1 :slug "slug1" :name "org1"}
               {:id 3 :slug "slug3" :name "org3"}]
              (org-invites-db/invites @re-frame.db/app-db)))
       (is (= [::foobar {}]
              @post-success-event)
           "The post-success event should be fired if available")))))

(deftest accept-invite-test
  (testing "When invite is accepted"
    (rf-test/run-test-sync
     (tu/stub-routing)
     (tu/initialize-db!)
     (swap! re-frame.db/app-db assoc :organization-invites {1 {:id 1 :slug "slug1" :name "org1"}
                                                            2 {:id 2 :slug "slug2" :name "org2"}
                                                            3 {:id 3 :slug "slug3" :name "org3"}})
     (tu/stub-xhrio {} true)
     (let [dispatched-event   (tu/stub-event ::org-events/fetch-organizations)
           post-success-event (tu/stub-event ::foobar)]
       (rf/dispatch [::org-invites-events/accept-invite "slug2" {:on-success [::foobar]}])
       (is (= [{:id 1 :slug "slug1" :name "org1"}
               {:id 3 :slug "slug3" :name "org3"}]
              (org-invites-db/invites @re-frame.db/app-db))
           "it should be removed from the pending invites map in the app db.")
       (is (= [::org-events/fetch-organizations]
              @dispatched-event)
           "it should trigger a re-fetch of organizations")
       (is (= [::foobar {}]
              @post-success-event)
           "The post-success event should be fired if available")))))
