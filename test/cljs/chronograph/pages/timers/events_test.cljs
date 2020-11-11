(ns chronograph.pages.timers.events-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests use-fixtures]]
            [chronograph-web.events.organization :as org-events]
            [chronograph.fixtures :as fixtures]
            [day8.re-frame.test :as rf-test]
            [chronograph.test-utils :as tu]
            [chronograph.specs]
            [re-frame.core :as rf]
            [re-frame.db :as db]
            [chronograph-web.pages.timers.events :as timers-events]
            [chronograph-web.pages.timers.subscriptions :as timers-subs]
            [chronograph-web.events.timer :as timer-events]
            [chronograph-web.subscriptions :as subs]
            [chronograph-web.effects]
            [chronograph-web.utils.time :as time]))

(use-fixtures :once fixtures/silence-logging fixtures/check-specs)

(def fake-now (let [d (js/Date.)]
                (.setYear d 2020)
                (.setMonth d 10)
                (.setDate d 5)
                d))

(deftest landing-page-test
  (testing "When the user navigates to the page, timers & invites should be fetched"
    (rf-test/run-test-sync
      ;; TODO: extract test setup stuff
     (tu/initialize-db!)
     (with-redefs [time/now (constantly fake-now)]
       (let [invited-orgs-stub (tu/stub-event ::timers-events/fetch-invited-orgs)
             fetch-timers-stub (tu/stub-event ::timer-events/fetch-timers)]
         (rf/dispatch [::timers-events/timers-page-navigated])
         (is (= [::timers-events/fetch-invited-orgs] @invited-orgs-stub))
         (is (= [::timer-events/fetch-timers {:day 5 :month 10 :year 2020}] @fetch-timers-stub))))))

  (testing "When invited orgs are fetched, db should contain them"
    (let [invited-orgs [{:id 1 :slug "slug1" :name "org1"}
                        {:id 2 :slug "slug2" :name "org2"}
                        {:id 3 :slug "slug3" :name "org3"}]]
      (rf-test/run-test-sync
       (tu/initialize-db!)
       (rf/reg-fx :http-xhrio
         (fn [_]
           (rf/dispatch [::timers-events/fetch-invited-orgs-success
                         invited-orgs])))
       (rf/dispatch [::timers-events/fetch-invited-orgs])
       (is (= invited-orgs
              @(rf/subscribe [::timers-subs/invites]))))))

  (testing "When invite is rejected, it should be removed from db"
    (rf-test/run-test-sync
     (tu/initialize-db!)
     (swap! db/app-db assoc :organization-invites {1 {:id 1 :slug "slug1" :name "org1"}
                                                   2 {:id 2 :slug "slug2" :name "org2"}
                                                   3 {:id 3 :slug "slug3" :name "org3"}})
     (rf/reg-fx :http-xhrio
       (fn [_]
         (rf/dispatch [::timers-events/reject-invite-succeeded 2])))
     (rf/dispatch [::timers-events/reject-invite 2])
     (is (= [{:id 1 :slug "slug1" :name "org1"}
             {:id 3 :slug "slug3" :name "org3"}]
            @(rf/subscribe [::timers-subs/invites])))))

  (testing "When invite is accepted"
    (rf-test/run-test-sync
     (tu/stub-routing)
     (tu/initialize-db!)
     (swap! re-frame.db/app-db assoc :organization-invites {1 {:id 1 :slug "slug1" :name "org1"}
                                                            2 {:id 2 :slug "slug2" :name "org2"}
                                                            3 {:id 3 :slug "slug3" :name "org3"}})
     (rf/reg-fx :http-xhrio
       (fn [_]
         (rf/dispatch [::timers-events/accept-invite-succeeded 2])))

     (let [dispatched-event (tu/stub-event ::org-events/fetch-organizations)]
       (rf/dispatch [::timers-events/accept-invite 2])
       (is (= [{:id 1 :slug "slug1" :name "org1"}
               {:id 3 :slug "slug3" :name "org3"}]
              @(rf/subscribe [::timers-subs/invites]))
           "it should be removed from the pending invites map in the app db.")
       (is (= [::org-events/fetch-organizations]
              @dispatched-event)
           "it should trigger a re-fetch of organizations")
       (is (= {:handler :root}
              @(rf/subscribe [::subs/current-page]))
           "the user remains on the landing page.")))))

(deftest timer-actions-test
  (testing "When starting a timer"
    (testing "succeeds, timers should be refetched"
      (rf-test/run-test-sync
       ;; TODO: extract test setup stuff
       (tu/initialize-db!)
       (with-redefs [time/now (constantly fake-now)]
         (let [fetch-timers-stub (tu/stub-event ::timer-events/fetch-timers)
               _                 (tu/stub-xhrio {} true)]
           (rf/dispatch [::timers-events/start-timer "foobar"])
           (is (= [::timer-events/fetch-timers {:day 5 :month 10 :year 2020} {}]
                  @fetch-timers-stub))))))

    (testing "fails, an error should be flashed"
      (rf-test/run-test-sync
       (tu/initialize-db!)
       (tu/stub-xhrio {} false)
       (let [error-params (tu/stub-effect :flash-error)]
         (rf/dispatch [::timers-events/start-timer "foobar"])
         (is (some? @error-params)
             "An error message should be flashed.")))))

  (testing "When stopping a timer"
    (testing "succeeds, timers should be refetched"
      (rf-test/run-test-sync
       ;; TODO: extract test setup stuff
       (tu/initialize-db!)
       (with-redefs [time/now (constantly fake-now)]
         (let [fetch-timers-stub (tu/stub-event ::timer-events/fetch-timers)
               _                 (tu/stub-xhrio {} true)]
           (rf/dispatch [::timers-events/stop-timer "foobar"])
           (is (= [::timer-events/fetch-timers {:day 5 :month 10 :year 2020} {}]
                  @fetch-timers-stub))))))

    (testing "fails, an error should be flashed"
      (rf-test/run-test-sync
       (tu/initialize-db!)
       (tu/stub-xhrio {} false)
       (let [error-params (tu/stub-effect :flash-error)]
         (rf/dispatch [::timers-events/stop-timer "foobar"])
         (is (some? @error-params)
             "An error message should be flashed.")))))

  (testing "When deleting a timer"
    (testing "succeeds, timers should be refetched"
      (rf-test/run-test-sync
       ;; TODO: extract test setup stuff
       (tu/initialize-db!)
       (with-redefs [time/now (constantly fake-now)]
         (let [fetch-timers-stub (tu/stub-event ::timer-events/fetch-timers)
               _                 (tu/stub-xhrio {} true)]
           (rf/dispatch [::timers-events/delete-timer "foobar"])
           (is (= [::timer-events/fetch-timers {:day 5 :month 10 :year 2020} {}]
                  @fetch-timers-stub))))))

    (testing "fails, an error should be flashed"
      (rf-test/run-test-sync
       (tu/initialize-db!)
       (tu/stub-xhrio {} false)
       (let [error-params (tu/stub-effect :flash-error)]
         (rf/dispatch [::timers-events/delete-timer "foobar"])
         (is (some? @error-params)
             "An error message should be flashed.")))))

  (testing "When creating a timer"
    (testing "succeeds"
      (rf-test/run-test-sync
       ;; TODO: extract test setup stuff
       (tu/initialize-db!)
       (with-redefs [time/now (constantly fake-now)]
         (let [fetch-timers-stub (tu/stub-event ::timer-events/fetch-timers)
               dismiss-stub      (tu/stub-event ::timers-events/dismiss-create-timer-widget)]
           (rf/dispatch [::timers-events/create-timer-succeeded])
           (is (= [::timer-events/fetch-timers {:day 5 :month 10 :year 2020}]
                  @fetch-timers-stub)
               "timers should be refetched")
           (is (= [::timers-events/dismiss-create-timer-widget]
                  @dismiss-stub)
               "the create timer widget should be dismissed")))))

    (testing "fails"
      (rf-test/run-test-sync
       (tu/initialize-db!)
       (tu/stub-xhrio {} false)
       (let [error-params (tu/stub-effect :flash-error)]
         (rf/dispatch [::timers-events/create-timer-failed])
         (is (some? @error-params)
             "An error message should be flashed."))))))
