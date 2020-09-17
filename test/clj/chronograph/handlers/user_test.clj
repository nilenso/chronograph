(ns chronograph.handlers.user-test
  (:require [chronograph.factories :as factories]
            [chronograph.fixtures :as fixtures]
            [chronograph.handlers.user :as hu]
            [clojure.test :refer :all]))

(use-fixtures :once fixtures/config fixtures/datasource)
(use-fixtures :each fixtures/clear-db)

(deftest me-when-user-exists-test
  (testing "Should return response with :user information if the user exists."
    (let [user (factories/create-user)
          request {:user user}]
      (is (= {:status 200
              :headers {}
              :body user}
             (hu/me request))))))
