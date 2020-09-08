(ns chronograph.handlers.user-test
  (:require [chronograph.auth :as auth]
            [chronograph.factories :as factories]
            [chronograph.fixtures :as fixtures]
            [chronograph.handlers.user :as hu]
            [chronograph.middleware :as middleware]
            [clojure.test :refer :all]))

(use-fixtures :once fixtures/config fixtures/datasource)
(use-fixtures :each fixtures/clear-db)

(def me-handler
  "Test handler to mimic authorized access to \"me\" routes. We do this because
  authorization is not standard, so we can't yet define a sensible test-handler."
  (-> hu/me
      middleware/wrap-authorized-user
      middleware/wrap-authenticated-user))

(deftest me-when-user-exists-test
  (testing "Should return response with :user information if the user exists."
    (let [user (factories/create-user)
          request {:cookies {"auth-token" {:value (auth/create-token
                                                   (:users/id user))}}}]
      (is (= {:status 200
              :headers {}
              :body user}
             (me-handler request))))))

(deftest me-when-user-doesnt-exist-test
  (testing "Should return a 401 if the user doesn't exist"
    (let [request {:cookies {"auth-token" {:value (auth/create-token
                                                   9876543210)}}}]
      (is (= {:status 401
              :headers {}
              :body {:error "Unauthorized"}}
             (me-handler request))))))
