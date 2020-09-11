(ns chronograph.middleware-test
  (:require [chronograph.auth :as auth]
            [chronograph.factories :as factories]
            [chronograph.fixtures :as fixtures]
            [chronograph.middleware :as middleware]
            [clojure.test :refer :all]
            [ring.util.response :as response]))

(use-fixtures :once fixtures/config fixtures/datasource fixtures/report-logging-only)
(use-fixtures :each fixtures/clear-db)

(deftest wrap-cookie-auth-test
  (let [user (factories/create-user)]
    (testing "Should return updated request having user's information, fetched from the DB, if the user is authentic."
      (is (= user
             (:user ((middleware/wrap-cookie-auth identity)
                     {:cookies {"auth-token" {:value (auth/create-token
                                                       (:users/id user))}}})))))

    (testing "Should return updated request with nil user, if the user is NOT authentic."
      (let [non-existent-user-id (+ (rand-int 100)
                                    1
                                    (:users/id user))]
        (is (nil?
              (:user ((middleware/wrap-cookie-auth identity)
                      {:cookies {"auth-token" {:value (auth/create-token non-existent-user-id)}}}))))))

    (testing "Should return updated request with nil user, if the auth-token has expired."
      (is (nil?
            (:user ((middleware/wrap-cookie-auth identity)
                    {:cookies {"auth-token" {:value (auth/create-token (:users/id user)
                                                                       -10)}}})))))))

(deftest wrap-header-auth-test
  (let [user (factories/create-user)]
    (testing "Should return updated request having user's information, fetched from the DB, if the user is authentic."
      (is (= user
             (:user ((middleware/wrap-header-auth identity)
                     {:headers {"authorization" (str "Bearer " (auth/create-token
                                                                 (:users/id user)))}})))))

    (testing "Should return updated request with nil user, if the user is NOT authentic."
      (let [non-existent-user-id (+ (rand-int 100)
                                    1
                                    (:users/id user))]
        (is (nil?
              (:user ((middleware/wrap-header-auth identity)
                      {:headers {"authorization" (str "Bearer " (auth/create-token non-existent-user-id))}}))))))

    (testing "Should return updated request with nil user, if the auth-token has expired."
      (is (nil?
            (:user ((middleware/wrap-header-auth identity)
                    {:headers {"authorization" (str "Bearer " (auth/create-token (:users/id user)
                                                                                 -10))}})))))))

(deftest wrap-require-user-info-test
  (let [fake-handler (constantly (-> (response/response "Success")
                                     (response/status 200)))]
    (testing "Calls the wrapped handler if the user key is present"
      (is (= {:status 200
              :headers {}
              :body   "Success"}
             ((middleware/wrap-require-user-info fake-handler)
              {:user "foobar"}))))

    (testing "Returns a 401 error if the user key is absent"
      (is (= 401
             (:status ((middleware/wrap-require-user-info fake-handler)
                       {})))))))
