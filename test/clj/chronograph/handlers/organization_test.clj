(ns chronograph.handlers.organization-test
  (:require [chronograph.domain.acl :as acl]
            [chronograph.factories :as factories]
            [chronograph.fixtures :as fixtures]
            [chronograph.handlers.organization :as organization]
            [clojure.spec.alpha :as s]
            [clojure.test :refer :all]
            [chronograph.middleware :as middleware]))

(use-fixtures :once fixtures/config fixtures/datasource)
(use-fixtures :each fixtures/clear-db)

(def create-organization
  "Test handler to mimic the relevant exception logging behaviour of the actual
  handler.We do this because error handling is not standardised, so we can't yet
  define a sensible test-handler."
  (middleware/wrap-exception-logging
   organization/create))


(deftest create-new-organization-first-time
  (testing "Creating a new organization returns a valid organization map in the response, and the creator is registered as admin in the ACL."
    (let [user (factories/create-user)
          response (create-organization {:body {:name "foo" :slug "bar"}
                                         :user user})]
      (is (= 200 (:status response)))
      (is (s/valid? :organizations/organization
                    (:body response)))
      (is (acl/admin? (:users/id user)
                      (:organizations/id (:body response)))))))


(deftest create-organization-when-slug-exists
  (testing "Creating an organization with pre-existing slug fails with an HTTP error."
    (let [user (factories/create-user)]
      (is (= {:status 500
              :headers {}
              :body {:error "Internal Server Error"}}
             (do (create-organization {:body {:name "foo" :slug "bar"}
                                       :user user})
                 (create-organization {:body {:name "foo" :slug "bar"}
                                       :user user})))))))


(deftest create-organization-disallows-unauthorised-user
  (testing "Creating an organization with unauthenticated user fails with HTTP error."
    (is (= {:status 401
            :headers {}
            :body {:error "Unauthorized"}}
           (create-organization {:body {:name "foo" :slug "bar"}
                                 :user nil})))))


(deftest create-organization-disallows-bad-request-params
  (testing "Creating an organization with non-conforming name and/or slug fails with HTTP error."
    (let [user (factories/create-user)]
      (is (= {:status 400
              :headers {}
              :body {:error "Bad name or slug."}}
             (create-organization {:body {:name "" :slug "foo"}
                                   :user (:users/id user)}))
          "Name cannot be empty.")
      (is (= {:status 400
              :headers {}
              :body {:error "Bad name or slug."}}
             (create-organization {:body {:name "foo bar" :slug ""}
                                   :user (:users/id user)}))
          "Slug cannot be empty.")
      (is (= {:status 400
              :headers {}
              :body {:error "Bad name or slug."}}
             (create-organization {:body {:name "foo bar" :slug "abc - 123 "}
                                   :user (:users/id user)}))
          "Slug must contain only lowercase letters, and optionally numbers or hypens. No whitespace allowed."))))
