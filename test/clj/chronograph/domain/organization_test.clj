(ns chronograph.domain.organization-test
  (:require [chronograph.domain.organization :as organization]
            [clojure.test :refer :all]
            [chronograph.domain.acl :as acl]
            [chronograph.domain.user :as user]
            [chronograph.fixtures :as fixtures]
            [chronograph.factories :as factories])
  (:import [org.postgresql.util PSQLException]))

(use-fixtures :once fixtures/config fixtures/datasource)
(use-fixtures :each fixtures/clear-db)

(deftest create-organization-disallows-reusing-slugs-test
  (testing "Creating an organization with the same slug as an existing one will fail"
    (let [{user-id :users/id} (factories/create-user)
          organization (factories/create-organization user-id)]
      (is (thrown-with-msg? PSQLException
                            #"duplicate key value violates unique constraint \"organizations_slug_key\""
                            (organization/create! organization user-id))))))

(deftest create-organization-sets-admin-test
  (testing "Creating an organization sets the creator as the admin"
    (let [{user-id :users/id} (factories/create-user)
          {organization-id :organizations/id} (factories/create-organization user-id) ]
      (is (true? (acl/admin? user-id organization-id))))))
