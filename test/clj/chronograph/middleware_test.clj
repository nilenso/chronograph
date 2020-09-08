(ns chronograph.middleware-test
  (:require [chronograph.auth :as auth]
            [chronograph.factories :as factories]
            [chronograph.fixtures :as fixtures]
            [chronograph.middleware :as middleware]
            [chronograph.utils.time :as time]
            [clojure.test :refer :all])
  (:import java.time.temporal.ChronoUnit))

(use-fixtures :once fixtures/config fixtures/datasource)
(use-fixtures :each fixtures/clear-db)

(deftest wrap-cookie-auth-when-user-authentic-test
  (testing "Should return updated request having user's information, fetched from the DB, if the user is authentic."
    (let [user (factories/create-user)]
      (is (= user
             (:user ((middleware/wrap-cookie-auth identity)
                     {:cookies {"auth-token" {:value (auth/create-token
                                                      (:users/id user))}}})))))))


(deftest wrap-cookie-auth-when-user-not-authentic-test
  (testing "Should return updated request with nil user, if the user is NOT authentic."
    (is (nil?
         (:user ((middleware/wrap-cookie-auth identity)
                 {:cookies {"auth-token" {:value (auth/create-token
                                                  9876543210)}}}))))))

(deftest wrap-cookie-auth-when-token-stale-test
  (testing "Should return updated request with nil user, if the auth-token has expired."
    (let [user (factories/create-user)]
      (is (nil?
           (:user ((middleware/wrap-cookie-auth identity)
                   {:cookies {"auth-token" {:value (auth/create-token
                                                    (:users/id user)
                                                    -10)}}})))))))
