(ns chronograph-web.pages.create-organization.events-test
  (:require [cljs.test :refer-macros [deftest is run-tests use-fixtures]]
            [chronograph-web.test-utils :as tu]
            [re-frame.core :as rf]
            [chronograph-web.fixtures :as fixtures]
            [chronograph-web.pages.create-organization.events :as create-org-events]
            [chronograph-web.subscriptions :as subs]))

(use-fixtures :once fixtures/silence-logging fixtures/check-specs)

(deftest create-organization-navigated
  (tu/rf-test "when the create organization page is navigated to"
    (rf/dispatch [::create-org-events/new-organization-page-navigated])
    (is (= :new-organization
           @(rf/subscribe [::subs/page-key])) "the page-key should be set")))

(deftest create-organization-form-submit-test
  (tu/rf-test "when the organization is created successfully"
    (let [slug         "slug"
          organization {:id   83
                        :name "Name"
                        :slug slug
                        :role "member"}]
      (rf/dispatch [::create-org-events/create-organization-succeeded
                    organization])
      (is (= {:route-params {:slug slug}
              :handler      :admin-page}
             @(rf/subscribe [::subs/current-page]))
          "the user should be routed to the organization-show page")
      (is (= @(rf/subscribe [::subs/organization slug])
             {:name "Name"
              :slug "slug"
              :id   83
              :role "member"})
          "should be able to lookup the organization by slug from the app-db")))

  (tu/rf-test "when organization creation fails"
    (let [slug         "slug"
          error-params (tu/stub-effect :flash-error)]
      (rf/dispatch [::create-org-events/create-organization-failed])
      (is (nil? @(rf/subscribe [::subs/organization slug])))
      (is (some? @error-params)
          "An error should be flashed"))))
