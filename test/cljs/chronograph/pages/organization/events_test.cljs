(ns chronograph.pages.organization.events-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests]]
            [day8.re-frame.test :as rf-test]
            [re-frame.core :as rf]
            [chronograph-web.pages.organization.events :as org-events]
            [chronograph-web.pages.organization.subscriptions :as org-subs]
            [chronograph-web.subscriptions :as subs]
            [chronograph.test-utils :as tu]))

(deftest organization-page-test
  (testing "When the page is mounted and the API calls succeed"
    (rf-test/run-test-sync
     (let [slug         "a-test-org"
           organization {:id         42
                         :name       "A Test Org"
                         :slug       slug
                         :created-at "2020-09-14T14:16:06.402873Z"
                         :updated-at "2020-09-14T14:16:06.402873Z"}]
       (rf/reg-fx :http-xhrio
         (fn [_]
           (rf/dispatch [::org-events/fetch-organization-success
                         organization])
           (rf/dispatch [::org-events/fetch-members-succeeded
                         {:invited [{:organization-id 42
                                     :email           "foo@bar.com"}]
                          :joined  [{:id               1
                                     :name             "Sandy"
                                     :email            "sandy@nilenso.com"}]}])))
       (tu/set-token (str "/organization/" slug))
       (rf/dispatch [::org-events/page-mounted])
       (is (= {:id         42
               :name       "A Test Org"
               :slug       "a-test-org"
               :created-at "2020-09-14T14:16:06.402873Z"
               :updated-at "2020-09-14T14:16:06.402873Z"}
              @(rf/subscribe [::subs/organization slug]))
           "The fetched organization should be in the DB")
       (is (= #{{:organization-id 42
                 :email           "foo@bar.com"}}
              @(rf/subscribe [::org-subs/invited-members]))
           "The invited members should be in the DB")
       (is (= #{{:id    1
                 :name  "Sandy"
                 :email "sandy@nilenso.com"}}
              @(rf/subscribe [::org-subs/joined-members]))
           "The joined members should be in the DB"))))

  (testing "When the invite member form succeeds"
    (rf-test/run-test-sync
     (tu/set-token "/organization/test-slug")
     (rf/dispatch [::org-events/fetch-organization-success
                   {:id   1
                    :slug "test-slug"}])
     (rf/reg-fx :http-xhrio
       (fn [_]
         (rf/dispatch [::org-events/invite-member-succeeded
                       {:organization-id 1
                        :email           "foo@bar.com"}])))
     (rf/dispatch [::org-events/invite-button-clicked])
     (is (= #{{:organization-id 1
               :email           "foo@bar.com"}}
            @(rf/subscribe [::org-subs/invited-members]))
         "The invited member should be in the DB")))

  (testing "When the organization fetch fails"
    (rf-test/run-test-sync
     (let [slug "a-test-org"]
       (rf/reg-fx :http-xhrio
         (fn [_]
           (rf/dispatch [::org-events/fetch-organization-fail])))
       (tu/set-token (str "/organization/" slug))
       (rf/dispatch [::org-events/page-mounted])
       (is (contains? @(rf/subscribe [::org-subs/page-errors]) ::org-events/error-org-not-found)
           "The reported error should be in the DB"))))

  (testing "When the invite member form fails"
    (rf-test/run-test-sync
     (rf/reg-fx :http-xhrio
       (fn [_]
         (rf/dispatch [::org-events/invite-member-failed])))
     (rf/dispatch [::org-events/invite-button-clicked])
     (is (contains? @(rf/subscribe [::org-subs/page-errors])
                    ::org-events/error-invite-member-failed)
         "The reported error should be in the DB")))

  (testing "When fetching org members fails"
    (rf-test/run-test-sync
     (rf/reg-fx :http-xhrio
       (fn [_]
         (rf/dispatch [::org-events/fetch-members-failed])))
     (rf/dispatch [::org-events/page-mounted])
     (is (contains? @(rf/subscribe [::org-subs/page-errors])
                    ::org-events/error-fetch-members-failed)
         "The reported error should be in the DB"))))
