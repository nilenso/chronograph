(ns chronograph.domain.acl-test
  (:require [chronograph.db.core :as db]
            [chronograph.db.acl :as db-acl]
            [chronograph.domain.acl :as acl]
            [chronograph.factories :as factories]
            [chronograph.fixtures :as fixtures]
            [chronograph.utils.time :as time]
            [clojure.test :refer :all]
            [next.jdbc :refer [with-transaction]])

  (:import org.postgresql.util.PSQLException))

(use-fixtures :once fixtures/config fixtures/datasource)
(use-fixtures :each fixtures/clear-db)

(deftest create-acl-test
  (testing "Can create an ACL for a user"
    (with-redefs [time/now (constantly (time/now))]
      (let [{admin-id :users/id} (factories/create-user)
            {user-id :users/id} (factories/create-user)
            {organization-id :organizations/id} (factories/create-organization admin-id)]
        (with-transaction [tx db/datasource]
          (acl/create! tx {:acls/user-id user-id
                           :acls/organization-id organization-id
                           :acls/role acl/admin}))
        (is (= #:acls{:user-id user-id
                      :organization-id organization-id
                      :role acl/admin
                      :created-at (time/now)
                      :updated-at (time/now)}
               (with-transaction [tx db/datasource]
                 (db-acl/find-by tx {:user-id  user-id :organization-id organization-id}))))))))

(deftest create-acl-when-user-organization-already-has-an-acl-test
  (testing "Creating an ACL for a user that already has one will fail"
    (let [{admin-id :users/id} (factories/create-user)
          {user-id :users/id} (factories/create-user)
          {organization-id :organizations/id} (factories/create-organization admin-id)]
      (with-transaction [tx db/datasource]
        (acl/create! tx {:acls/user-id user-id
                         :acls/organization-id organization-id
                         :acls/role acl/member}))
      (is (thrown-with-msg? PSQLException
                            #"duplicate key value violates unique constraint \"acls_user_id_organization_id_idx\""
                            (with-transaction [tx db/datasource]
                              (acl/create! tx {:acls/user-id user-id
                                               :acls/organization-id organization-id
                                               :acls/role acl/member})))))))

(deftest admin?-test
  (testing "Can check whether a user is a admin of an organization"
    (let [{admin-id :users/id} (factories/create-user)
          {user-id :users/id} (factories/create-user)
          {organization-id :organizations/id} (factories/create-organization admin-id)]
      (with-transaction [tx db/datasource]
        (acl/create! tx {:acls/user-id user-id
                         :acls/organization-id organization-id
                         :acls/role acl/admin}))
      (with-transaction [tx db/datasource]
        (is (acl/admin? tx user-id organization-id))))))

(deftest belongs-to-org?-test
  (let [user (factories/create-user)
        organization (factories/create-organization (:users/id user))]

    (testing "when a user is an admin of the organization"
      (with-transaction [tx db/datasource]
        (is (acl/belongs-to-org? tx
                                 (:users/id user)
                                 (:organizations/id organization)))))

    (testing "when a user does not exist"
      (with-transaction [tx db/datasource]
        (is (not (acl/belongs-to-org? tx
                                      (Long/MAX_VALUE)
                                      (:organizations/id organization))))))

    (testing "when an organization does not exist"
      (with-transaction [tx db/datasource]
        (is (not (acl/belongs-to-org? tx (:users/id user) nil)))))

    (testing "when a user is not an 'admin' of the organization"
      (let [member (factories/create-user)]
        (with-transaction [tx db/datasource]
          (acl/create! tx
                       {:acls/user-id (:users/id member)
                        :acls/organization-id (:organizations/id organization)
                        :acls/role acl/member}))
        (with-transaction [tx db/datasource]
          (is (acl/belongs-to-org? tx
                                   (:users/id member)
                                   (:organizations/id organization))))))))
