(ns chronograph.domain.acl-test
  (:require [chronograph.db.acl :as db-acl]
            [chronograph.domain.acl :as acl]
            [chronograph.domain.organization :as organization]
            [chronograph.factories :as factories]
            [chronograph.fixtures :as fixtures]
            [clojure.test :refer :all])
  (:import org.postgresql.util.PSQLException))

(use-fixtures :once fixtures/config fixtures/datasource)
(use-fixtures :each fixtures/clear-db)

(deftest create-acl-test
  (testing "Can create an ACL for a user"
    (let [{admin-id :users/id} (factories/create-user)
          {user-id :users/id} (factories/create-user)
          {organization-id :organizations/id} (factories/create-organization admin-id)]
      (acl/create! {:user-id user-id
                    :organization-id organization-id
                    :role acl/admin})
      (is (= #:acls{:user-id user-id
                    :organization-id organization-id
                    :role acl/admin}
             (db-acl/find-acl user-id organization-id))))))

(deftest create-acl-when-user-organization-already-has-an-acl-test
  (testing "Creating an ACL for a user that already has one will fail"
    (let [{admin-id :users/id} (factories/create-user)
          {user-id :users/id} (factories/create-user)
          {organization-id :organizations/id} (factories/create-organization admin-id)]
      (acl/create! {:user-id user-id
                    :organization-id organization-id
                    :role acl/member})
      (is (thrown-with-msg? PSQLException
                            #"duplicate key value violates unique constraint \"acls_user_id_organization_id_idx\""
                            (acl/create! {:user-id user-id
                                          :organization-id organization-id
                                          :role acl/member}))))))

(deftest admin?-test
  (testing "Can check whether a user is a admin of an organization"
    (let [{admin-id :users/id} (factories/create-user)
          {user-id :users/id} (factories/create-user)
          {organization-id :organizations/id} (factories/create-organization admin-id)]
      (acl/create! {:user-id user-id
                    :organization-id organization-id
                    :role acl/admin})
      (is (acl/admin? user-id organization-id)))))
