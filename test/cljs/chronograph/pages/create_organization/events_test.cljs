(ns chronograph.pages.create-organization.events-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests use-fixtures]]
            [day8.re-frame.test :as rf-test]
            [re-frame.core :as rf]
            [chronograph.specs]
            [chronograph-web.pages.create-organization.events :as create-organization-events]
            [chronograph-web.subscriptions :as subs]
            [chronograph.test-utils :as tu]
            [chronograph.fixtures :as fixtures]))

(use-fixtures :once fixtures/check-specs)

(deftest create-organization-form-submit-test
  (testing "when the organization is created successfully"
    (rf-test/run-test-sync
     (tu/initialize-db!)
     (tu/stub-routing)
     (let [name "Name"
           slug "slug"
           id 83
           organization {:id id :name name :slug slug}]
       (rf/dispatch [::create-organization-events/create-organization-succeeded
                     organization])
       (is (= {:route-params {:slug slug}
               :handler      :organization-show}
              @(rf/subscribe [::subs/current-page]))
           "the user should be routed to the organization-show page")
       (is (= @(rf/subscribe [::subs/organization slug])
              {:name "Name"
               :slug "slug"
               :id 83})
           "should be able to lookup the organization by slug from the app-db"))))

  (testing "when organization creation fails"
    (rf-test/run-test-sync
     (tu/initialize-db!)
     (let [slug "slug"]
       (rf/dispatch [::create-organization-events/create-organization-failed])
       (is (nil? @(rf/subscribe [::subs/organization slug]))))))

  (testing "when organization creation is ongoing"
    (rf-test/run-test-sync
     (tu/initialize-db!)
     (let [slug "slug"]
       (js/setTimeout #(rf/dispatch
                        [::create-organization-events/create-organization-failed])
                      1000)
       (is (nil? @(rf/subscribe [::subs/organization slug]))
           "the organization should not exist in the db")))))
