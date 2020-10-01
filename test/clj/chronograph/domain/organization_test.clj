(ns chronograph.domain.organization-test
  (:require [chronograph.db.core :as db]
            [chronograph.domain.acl :as acl]
            [chronograph.factories :as factories]
            [chronograph.fixtures :as fixtures]
            [clojure.test :refer :all]
            [next.jdbc :refer [with-transaction]]
            [chronograph.domain.organization :as organization])
  (:import [org.postgresql.util PSQLException]))

(use-fixtures :once fixtures/config fixtures/datasource)
(use-fixtures :each fixtures/clear-db)

(deftest create-organization-disallows-reusing-slugs-test
  (testing "Creating an organization with the same slug as an existing one will fail"
    (let [{user-id :users/id} (factories/create-user)
          organization (factories/create-organization user-id)]
      (with-transaction [tx db/datasource]
        (is (thrown-with-msg? PSQLException
                              #"duplicate key value violates unique constraint \"organizations_slug_key\""
                              (organization/create! tx organization user-id)))))))

(deftest create-organization-sets-admin-test
  (testing "Creating an organization sets the creator as the admin"
    (let [{user-id :users/id} (factories/create-user)
          {organization-id :organizations/id} (factories/create-organization user-id)]
      (with-transaction [tx db/datasource]
        (is (true? (acl/admin? tx user-id organization-id)))))))

(deftest find-one-organization-test
  (testing "when users try to look up organization information"
    (let [;; user-one and their organization
          {user-one :users/id} (factories/create-user)
          organization-one (factories/create-organization user-one)
          ;; user-two and their organization
          {user-two :users/id} (factories/create-user)
          organization-two (factories/create-organization user-two)]
      (with-transaction [tx db/datasource]
        (is (= organization-one
               (organization/find-if-authorized tx
                                                (:organizations/slug organization-one)
                                                user-one))
            "user-one can look up the organization they belong to")
        (is (nil? (organization/find-if-authorized tx
                                                   (:organizations/slug organization-two)
                                                   user-one))
            "user-one CANNOT look up an organization they don't belong to")
        (is (nil? (organization/find-if-authorized tx
                                                   "this-does-not-exist"
                                                   user-one))
            "find-if-authorized returns nil when the organization does not exist")
        (is (nil? (organization/find-if-authorized tx
                                                   (:organizations/slug organization-one)
                                                   (Long/MAX_VALUE)))
            "find-one returns nil when the user does not exist")))))
