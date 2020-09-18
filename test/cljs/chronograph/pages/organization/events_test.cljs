(ns chronograph.pages.organization.events-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests]]
            [day8.re-frame.test :as rf-test]
            [re-frame.core :as rf]
            [chronograph-web.pages.organization.events :as org-events]
            [chronograph-web.events.routing :as routing-events]
            [chronograph-web.subscriptions :as subs]))


(deftest get-organization-test
  (testing "When the organization is fetched successfully"
    (rf-test/run-test-sync
     (let [slug "a-test-org"
           organization {:id 42
                         :name "A Test Org"
                         :slug slug
                         :created-at "2020-09-14T14:16:06.402873Z"
                         :updated-at "2020-09-14T14:16:06.402873Z"}]
       (rf/reg-fx :http-xhrio
                  (fn [_]
                    (rf/dispatch [::org-events/fetch-organization-success
                                  organization])))
       (rf/dispatch [::routing-events/set-page {:slug slug :handler :organization-show}])
       (rf/dispatch [::org-events/fetch-organization slug])
       (is (= {:id 42
               :name "A Test Org"
               :slug "a-test-org"
               :created-at "2020-09-14T14:16:06.402873Z"
               :updated-at "2020-09-14T14:16:06.402873Z"}
              @(rf/subscribe [::subs/organization slug]))))))

  (testing "When the organization fetch fails"
    (rf-test/run-test-sync
     (let [slug "a-test-org"]
       (rf/reg-fx :http-xhrio
                  (fn [_]
                    (rf/dispatch [::org-events/fetch-organization-fail])))
       (rf/dispatch [::routing-events/set-page {:slug slug :handler :organization-show}])
       (is (nil? @(rf/subscribe [::subs/organization slug])))))))
