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
      (is (true? (acl/admin? db/datasource user-id organization-id))))))

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

(deftest for-user-test
  (testing "Returns the list of organizations a user belongs to"
    (let [user (factories/create-user)
          organization1 (factories/create-organization (:users/id user))
          organization2 (factories/create-organization (:users/id user))]
      (with-transaction [tx db/datasource]
        (let [user-organizations (organization/for-user tx user)]
          (is (= 2 (count user-organizations)))
          (is (= #{(:organizations/id organization1)
                   (:organizations/id organization2)}
                 (set (map :organizations/id user-organizations))))))))
  (testing "Does not return organizations to which the user does not belong"
    (let [user (factories/create-user)
          other-user (factories/create-user)
          organization1 (factories/create-organization (:users/id user))
          _organization2 (factories/create-organization (:users/id other-user))]
      (with-transaction [tx db/datasource]
        (let [user-organizations (organization/for-user tx user)]
          (is (= 1 (count user-organizations)))
          (is (= #{(:organizations/id organization1)}
                 (set (map :organizations/id user-organizations)))))))))

(deftest find-by-slug-test
  (testing "it looks up organizations by slug"
    (let [{user-one :users/id} (factories/create-user)
          org (factories/create-organization user-one)]
      (is (= org
             (organization/find-by-slug db/datasource (:organizations/slug org)))))))

(deftest members-test
  (testing "it finds all members of an organization, whether admin or member"
    (let [{user-id-1 :users/id :as user1} (factories/create-user)
          {org-id :organizations/id} (factories/create-organization user-id-1)
          {user-id-2 :users/id :as user2} (factories/create-user)
          {user-id-3 :users/id :as user3} (factories/create-user)
          {user-id-4 :users/id :as user4} (factories/create-user)
          {org-id-2 :organizations/id} (factories/create-organization user-id-4)]
      (acl/create! db/datasource {:user-id         user-id-2
                                  :organization-id org-id
                                  :role            acl/member})
      (acl/create! db/datasource {:user-id         user-id-3
                                  :organization-id org-id
                                  :role            acl/member})
      (is (= #{user1 user2 user3}
             (set (organization/members db/datasource org-id))))
      (is (= #{user4}
             (set (organization/members db/datasource org-id-2)))))))
