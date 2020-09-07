(ns chronograph.handlers.organization-test
  (:require [chronograph.domain.acl :as acl]
            [chronograph.factories :as factories]
            [chronograph.fixtures :as fixtures]
            [chronograph.handlers.organization :as organization]
            [clojure.spec.alpha :as s]
            [clojure.test :refer :all])
  (:import org.postgresql.util.PSQLException))

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


(deftest create-organization-when-name-exists
  (testing "Creating an organization with pre-existing name works if slugs are different."
    (let [user (factories/create-user)]
      (is (s/valid? :organizations/organization
                    (:body (do (organization/create {:body {:name "foo" :slug "bar"}
                                                     :user user})
                               (organization/create {:body {:name "foo" :slug "bar-baz"}
                                                     :user user}))))))))


(deftest create-organization-when-slug-exists
  (testing "Creating an organization with pre-existing slug fails."
    (let [user (factories/create-user)]
      (is (thrown-with-msg? PSQLException
                            #"ERROR: duplicate key value violates unique constraint \"organizations_slug_key\""
                            (do (organization/create {:body {:name "foo" :slug "bar"}
                                                      :user user})
                                (organization/create {:body {:name "foo" :slug "bar"}
                                                      :user user})))))))


(deftest create-organization-disallows-unauthorised-user
  (testing "Creating an organization with unauthenticated user fails with HTTP error."
    (is (= {:status 401
            :headers {}
            :body {:error "Unauthorized"}}
           (organization/create {:body {:name "foo" :slug "bar"}
                                 :user nil})))))


(deftest create-organization-disallows-bad-request-params
  (testing "Creating an organization with non-conforming name and/or slug fails with HTTP error."
    (let [user (factories/create-user)]
      (is (= {:status 400
              :headers {}
              :body {:error "Bad name or slug."}}
             (organization/create {:body {:name "" :slug "foo"}
                                   :user (:users/id user)}))
          "Name cannot be empty.")
      (is (= {:status 400
              :headers {}
              :body {:error "Bad name or slug."}}
             (organization/create {:body {:name "foo bar" :slug ""}
                                   :user (:users/id user)}))
          "Slug cannot be empty.")
      (is (= {:status 400
              :headers {}
              :body {:error "Bad name or slug."}}
             (organization/create {:body {:name "foo bar" :slug "abc - 123 "}
                                   :user (:users/id user)}))
          "Slug must contain only lowercase letters, and optionally numbers or hypens. No whitespace allowed."))))
