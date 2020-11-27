(ns chronograph-web.events.organization-test
  (:require [cljs.test :refer-macros [deftest is run-tests use-fixtures]]
            [chronograph-web.fixtures :as fixtures]
            [chronograph-web.test-utils :as tu]
            [re-frame.core :as rf]
            [re-frame.db]
            [chronograph-web.events.organization :as org-events]
            [chronograph-web.db.organization :as org-db]))

(use-fixtures :once fixtures/silence-logging fixtures/check-specs)

(deftest fetch-organizations-test
  (tu/rf-test "When organizations are fetched, they should be present in the DB"
    (let [orgs               [{:id 1 :slug "slug1" :name "org1" :role "member"}
                              {:id 2 :slug "slug2" :name "org2" :role "member"}
                              {:id 3 :slug "slug3" :name "org3" :role "member"}]
          post-success-event (tu/stub-event ::foobar)]
      (tu/initialize-db!)
      (tu/stub-xhrio orgs true)
      (rf/dispatch [::org-events/fetch-organizations {:on-success [::foobar]}])
      (is (= orgs
             (org-db/organizations @re-frame.db/app-db)))
      (is (= [::foobar orgs]
             @post-success-event)
          "The post-success event should be fired if available"))))
