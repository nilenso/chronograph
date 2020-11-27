(ns chronograph-web.pages.welcome.events-test
  (:require [cljs.test :refer-macros [deftest is run-tests use-fixtures]]
            [chronograph-web.fixtures :as fixtures]
            [chronograph-web.test-utils :as tu]
            [re-frame.core :as rf]
            [chronograph-web.pages.welcome.events :as welcome-events]
            [re-frame.db]
            [chronograph-web.db.organization :as org-db]))

(use-fixtures :once fixtures/silence-logging fixtures/check-specs)

(deftest after-fetch-organizations-test
  (tu/rf-test "when there are organizations in the DB, it should redirect to the timers page"
    (tu/set-token "/welcome")
    (swap! re-frame.db/app-db
           org-db/set-organizations
           [{:id 1 :slug "slug1" :name "org1" :role "member"}
            {:id 2 :slug "slug2" :name "org2" :role "member"}
            {:id 3 :slug "slug3" :name "org3" :role "member"}])
    (rf/dispatch [::welcome-events/after-fetch-organizations])
    (is (= :timers-list
           (get-in @re-frame.db/app-db [:page :handler]))))

  (tu/rf-test "when there are no organizations in the DB, it should remain on the current page"
    (tu/set-token "/welcome")
    (rf/dispatch [::welcome-events/after-fetch-organizations])
    (is (= :welcome-page
           (get-in @re-frame.db/app-db [:page :handler])))))

(deftest after-invite-accepted-test
  (tu/rf-test "it should redirect to the accepted invite's timers page"
    (tu/set-token "/welcome")
    (rf/dispatch [::welcome-events/after-invite-accepted "foobar"])
    (is (= {:handler      :timers-list
            :route-params {:slug "foobar"}}
           (:page @re-frame.db/app-db)))))
