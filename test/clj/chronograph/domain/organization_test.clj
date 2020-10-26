(ns chronograph.domain.organization-test
  (:require [chronograph.db.core :as db]
            [chronograph.domain.acl :as acl]
            [chronograph.factories :as factories]
            [chronograph.fixtures :as fixtures]
            [clojure.test :refer :all]
            [next.jdbc :refer [with-transaction]]
            [chronograph.domain.organization :as organization]
            [chronograph.domain.invite :as invite]
            [chronograph.test-utils :as tu])
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
  (testing "Returns the list of organizations a user belongs to, along with roles"
    (tu/with-fixtures [fixtures/clear-db]
      (let [user          (factories/create-user)
            organization1 (factories/create-organization (:users/id user))
            organization2 (factories/create-organization (:users/id user))
            {org-id-3 :organizations/id :as organization3} (factories/create-user-and-organization)]
        (acl/create! db/datasource #:acls{:user-id         (:users/id user)
                                          :organization-id org-id-3
                                          :role            acl/member})
        (with-transaction [tx db/datasource]
          (let [user-organizations (organization/for-user tx user)]
            (is (= #{(assoc organization1
                            :acls/role acl/admin)
                     (assoc organization2
                            :acls/role acl/admin)
                     (assoc organization3
                            :acls/role acl/member)}
                   (set user-organizations))))))))

  (testing "Does not return organizations to which the user does not belong"
    (tu/with-fixtures [fixtures/clear-db]
      (let [user           (factories/create-user)
            other-user     (factories/create-user)
            organization1  (factories/create-organization (:users/id user))
            _organization2 (factories/create-organization (:users/id other-user))]
        (with-transaction [tx db/datasource]
          (let [user-organizations (organization/for-user tx user)]
            (is (= #{(:organizations/id organization1)}
                   (set (map :organizations/id user-organizations))))))))))

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
      (acl/create! db/datasource {:acls/user-id         user-id-2
                                  :acls/organization-id org-id
                                  :acls/role            acl/member})
      (acl/create! db/datasource {:acls/user-id         user-id-3
                                  :acls/organization-id org-id
                                  :acls/role            acl/member})
      (is (= #{user1 user2 user3}
             (set (organization/members db/datasource org-id))))
      (is (= #{user4}
             (set (organization/members db/datasource org-id-2)))))))

(deftest find-invited-test
  (testing "It finds orgs that the user has been invited to"
    (let [{user-id-1 :users/id} (factories/create-user)
          {org-id :organizations/id :as org1} (factories/create-organization user-id-1)
          {org-id-2 :organizations/id :as org2} (factories/create-organization user-id-1)
          {user-id-2 :users/id
           email     :users/email} (factories/create-user)]
      (factories/create-organization user-id-2) ; This should not show in up the invited orgs
      (invite/find-or-create! db/datasource org-id email)
      (invite/find-or-create! db/datasource org-id-2 email)
      (is (= #{org1 org2}
             (set (organization/find-invited db/datasource email)))))))
