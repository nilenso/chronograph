(ns chronograph.handlers.organization-test
  (:require [chronograph.domain.acl :as acl]
            [chronograph.factories :as factories]
            [chronograph.fixtures :as fixtures]
            [chronograph.handlers.organization :as organization]
            [clojure.spec.alpha :as s]
            [clojure.test :refer :all])
  (:import (org.postgresql.util PSQLException)))

(use-fixtures :once fixtures/config fixtures/datasource)
(use-fixtures :each fixtures/clear-db)

(deftest create-new-organization-first-time
  (testing "Creating a new organization returns a valid organization map in the response, and the creator is registered as admin in the ACL."
    (let [user (factories/create-user)
          response (organization/create {:body {:name "foo" :slug "bar"}
                                         :user user})]
      (is (= 200 (:status response)))
      (is (s/valid? :organizations/organization
                    (:body response)))
      (is (acl/admin? (:users/id user)
                      (:organizations/id (:body response)))))))

(deftest create-organization-when-slug-exists
  (testing "Creating an organization with pre-existing slug throws an exception."
    (let [user (factories/create-user)]
      (organization/create {:body {:name "foo" :slug "bar"}
                            :user user})
      (is (thrown? PSQLException (organization/create {:body {:name "foo" :slug "bar"}
                                                       :user user}))))))

(deftest create-organization-disallows-bad-request-params
  (testing "Creating an organization with non-conforming name and/or slug fails with HTTP error."
    (let [user (factories/create-user)]
      (is (= {:status 400
              :headers {}
              :body {:error "Bad name or slug."}}
             (organization/create {:body {:name "" :slug "foo"}
                                   :user user}))
          "Name cannot be empty.")
      (is (= {:status 400
              :headers {}
              :body {:error "Bad name or slug."}}
             (organization/create {:body {:name "foo bar" :slug ""}
                                   :user user}))
          "Slug cannot be empty.")
      (is (= {:status 400
              :headers {}
              :body {:error "Bad name or slug."}}
             (organization/create {:body {:name "foo bar" :slug "abc - 123 "}
                                   :user user}))
          "Slug must contain only lowercase letters, and optionally numbers or hypens. No whitespace allowed.")
      (is (= {:status 400
              :headers {}
              :body {:error "Bad name or slug."}}
             (organization/create
              {:body {:name "foo bar"
                      :slug (->> (repeat 257 "a")
                                 (apply str))}
               :user user}))
          "Slug length must not exceed 256 characters.")
      (let [response (organization/create
                      {:body {:name "foo bar"
                              :slug (->> (repeat (/ 256 4) "a-34")
                                         (apply str))}
                       :user user})]
        (is (= 200 (:status response)))
        (is (s/valid? :organizations/organization
                      (:body response))
            "Slug length can be exactly 256 characters at most.")))))
