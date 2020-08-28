(ns chronograph.handlers.user-test
  (:require [clojure.test :refer :all]
            [chronograph.handlers.user :as hu]
            [chronograph.fixtures :as fixtures]
            [mock-clj.core :as mc]
            [chronograph.auth :as auth]
            [chronograph.domain.user :as user]))

(use-fixtures :once fixtures/config fixtures/datasource)
(use-fixtures :each fixtures/clear-db)

(deftest me-when-user-exists-test
  (testing "Should retrieve the authenticated user's information from the DB if they exist"
    (let [{:keys [id]} (user/find-or-create-google-user! "google-123"
                                                         "Foo Bar"
                                                         "foo@bar.baz"
                                                         "https://foo.png")]
      (mc/with-mock [auth/verify-token {:id id}]
        (is (= {:id        id
                :name      "Foo Bar"
                :email     "foo@bar.baz"
                :photo-url "https://foo.png"}
               (:body (hu/me {:cookies {"auth-token" {:value "stub"}}}))))))))

(deftest me-when-user-doesnt-exist-test
  (testing "Should return a 401 if the user doesn't exist"
    (mc/with-mock [auth/verify-token {:id 123}]
      (is (= {:status 401
              :body   {:error "Unauthorized"}}
             (-> (hu/me {:cookies {"auth-token" {:value "stub"}}})
                 (select-keys [:status :body])))))))
