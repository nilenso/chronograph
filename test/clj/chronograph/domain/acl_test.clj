(ns chronograph.domain.acl-test
  (:require [chronograph.db.acl :as db-acl]
            [chronograph.domain.acl :as acl]
            [chronograph.factories :as factories]
            [chronograph.fixtures :as fixtures]
            [clojure.test :refer :all]
            [chronograph.db.core :as db])
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
      (is (acl/admin? db/datasource user-id organization-id)))))

(deftest belongs-to-org?-test
  (let [user (factories/create-user)
        organization (factories/create-organization (:users/id user))]

    (testing "when a user is an admin of the organization"
      (is (acl/belongs-to-org? (:users/id user)
                               (:organizations/id organization))))

    (testing "when a user does not exist"
      (is (not (acl/belongs-to-org? (Long/MAX_VALUE)
                                    (:organizations/id organization)))))

    (testing "when an organization does not exist"
      (is (not (acl/belongs-to-org? (:users/id user)
                                    nil))))

    (testing "when a user is not an 'admin' of the organization"
      (let [member (factories/create-user)]
        (acl/create! {:user-id (:users/id member)
                      :organization-id (:organizations/id organization)
                      :role acl/member})
        (is (acl/belongs-to-org? (:users/id member)
                                 (:organizations/id organization)))))))
