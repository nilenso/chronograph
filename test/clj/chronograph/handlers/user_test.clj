(ns chronograph.handlers.user-test
  (:require [clojure.test :refer :all]
            [chronograph.handlers.user :as hu]
            [chronograph.fixtures :as fixtures]
            [mock-clj.core :as mc]
            [chronograph.auth :as auth]
            [chronograph.domain.user :as user]
            [chronograph.factories :as factories]
            [chronograph.utils.time :as time]))

(use-fixtures :once fixtures/config fixtures/datasource)
(use-fixtures :each fixtures/clear-db)

(deftest me-when-user-exists-test
  (testing "Should retrieve the authenticated user's information from the DB if they exist"
    (with-redefs [time/now (constantly (time/now))]
      (let [{:keys [users/id users/name users/email users/photo-url]} (factories/create-user)
            now (time/now)]
        (mc/with-mock [auth/verify-token {:id id}]
          (is (= #:users{:id        id
                         :name      name
                         :email     email
                         :photo-url photo-url
                         :created-at now
                         :updated-at now}
                 (:body (hu/me {:cookies {"auth-token" {:value "stub"}}})))))))))

(deftest me-when-user-doesnt-exist-test
  (testing "Should return a 401 if the user doesn't exist"
    (mc/with-mock [auth/verify-token {:id 123}]
      (is (= {:status 401
              :body   {:error "Unauthorized"}}
             (-> (hu/me {:cookies {"auth-token" {:value "stub"}}})
                 (select-keys [:status :body])))))))
