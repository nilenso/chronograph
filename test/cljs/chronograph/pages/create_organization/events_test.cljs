(ns chronograph.pages.create-organization.events-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests]]
            [day8.re-frame.test :as rf-test]
            [re-frame.core :as rf]
            [chronograph.specs]
            [chronograph-web.pages.create-organization.events :as create-organization-events]
            [chronograph-web.events.routing :as routing-events]
            [chronograph-web.subscriptions :as subs]))

(deftest create-organization-form-update-test
  (testing "when the name is updated"
    (rf-test/run-test-sync
     (let [name "Nom"]
       (rf/dispatch [::create-organization-events/create-organization-form-update :name name])
       (is (= @(rf/subscribe [::subs/create-organization-form])
              {:status :editing
               :form-params {:name "Nom"}})))))

  (testing "when the slug is updated"
    (rf-test/run-test-sync
     (let [slug "slug"]
       (rf/dispatch [::create-organization-events/create-organization-form-update :slug slug])
       (is (= @(rf/subscribe [::subs/create-organization-form])
              {:status :editing
               :form-params {:slug "slug"}})))))

  (testing "when the name is invalid"
    (rf-test/run-test-sync
     (let [name ""]
       (rf/dispatch [::create-organization-events/create-organization-form-update :name name])
       (is (= @(rf/subscribe [::subs/create-organization-form])
              {:status :editing
               :form-params {:name ""}})))))

  (testing "when the slug is invalid"
    (rf-test/run-test-sync
     (let [slug "SLU UG"]
       (rf/dispatch [::create-organization-events/create-organization-form-update :slug slug])
       (is (= @(rf/subscribe [::subs/create-organization-form])
              {:status :editing
               :form-params {:slug "SLU UG"}}))))))

(deftest create-organization-form-submit-test
  (testing "when the organization is created successfully"
    (rf-test/run-test-sync
     (let [name "Name"
           slug "slug"
           id 83
           organization {:id id :name name :slug slug}
           token (atom nil)]
       (rf/reg-fx :http-xhrio
         (fn [_]
           (rf/dispatch [::create-organization-events/create-organization-succeeded
                         organization])))
       (rf/reg-fx :history-token #(reset! token %))
       (rf/dispatch [::routing-events/set-page :organization-new])
       (rf/dispatch [::routing-events/set-token "/organization/new"])
       (rf/dispatch [::create-organization-events/create-organization-form-update :name name])
       (rf/dispatch [::create-organization-events/create-organization-form-update :slug slug])
       (rf/dispatch [::create-organization-events/create-organization-form-submit])
       (is (= (:status @(rf/subscribe [::subs/create-organization-form]))
              :created)
           "the create-organization-form state should be :created")
       (is (= @token "/organization/slug")
           "the user's browser path should be set to the organization show page")
       (is (= @(rf/subscribe [::subs/organization slug])
              {:name "Name"
               :slug "slug"
               :id 83})
           "should be able to lookup the organization by slug from the app-db"))))

  (testing "when organization creation fails"
    (rf-test/run-test-sync
     (let [name "Name"
           slug "slug"]
       (rf/reg-fx :http-xhrio
         (fn [_]
           (rf/dispatch [::create-organization-events/create-organization-failed])))
       (rf/dispatch [::routing-events/set-page :organization-new])
       (rf/dispatch [::routing-events/set-token "/organization/new"])
       (rf/dispatch [::create-organization-events/create-organization-form-update :name name])
       (rf/dispatch [::create-organization-events/create-organization-form-update :slug slug])
       (rf/dispatch [::create-organization-events/create-organization-form-submit])
       (is (= (:status @(rf/subscribe [::subs/create-organization-form]))
              :failed)
           "the create-organization-form state should be :failed")
       (is (nil? @(rf/subscribe [::subs/organization slug]))))))

  (testing "when organization creation is ongoing"
    (rf-test/run-test-sync
     (let [name "Name"
           slug "slug"]
       (rf/reg-fx :http-xhrio
         (fn [_]
           (js/setTimeout #(rf/dispatch
                            [::create-organization-events/create-organization-failed])
                          1000)))
       (rf/dispatch [::routing-events/set-page :organization-new])
       (rf/dispatch [::routing-events/set-token "/organization/new"])
       (rf/dispatch [::create-organization-events/create-organization-form-update :name name])
       (rf/dispatch [::create-organization-events/create-organization-form-update :slug slug])
       (rf/dispatch [::create-organization-events/create-organization-form-submit])
       (is (= (:status @(rf/subscribe [::subs/create-organization-form]))
              :creating)
           "the create-organization-form state should be :creating")
       (is (nil? @(rf/subscribe [::subs/organization slug]))
           "the organization should not exist in the db")))))
