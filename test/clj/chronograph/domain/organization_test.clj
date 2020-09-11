(ns chronograph.domain.organization-test
  (:require [chronograph.domain.organization :as organization]
            [clojure.test :refer :all]
            [chronograph.domain.acl :as acl]
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
          {organization-id :organizations/id} (factories/create-organization user-id)]
      (is (true? (acl/admin? user-id organization-id))))))

(deftest find-one-organization-test
  (testing "when users try to look up organization information"
    (let [;; user-one and their organization
          {user-one :users/id} (factories/create-user)
          organization-one (factories/create-organization user-one)
          ;; user-two and their organization
          {user-two :users/id} (factories/create-user)
          organization-two (factories/create-organization user-two)]
      (is (= organization-one
             (organization/find-if-authorized (:organizations/slug organization-one)
                                              user-one))
          "user-one can look up the organization they belong to")
      (is (nil? (organization/find-if-authorized (:organizations/slug organization-two)
                                                 user-one))
          "user-one CANNOT look up an organization they don't belong to")
      (is (nil? (organization/find-if-authorized "this-does-not-exist"
                                                 user-one))
          "find-if-authorized returns nil when the organization does not exist")
      (is (nil? (organization/find-if-authorized (:organizations/slug organization-one)
                                                 (Long/MAX_VALUE)))
          "find-one returns nil when the user does not exist"))))

(deftest join-requests-disabled-by-default-test
  (testing "Join requests are disabled on new organizations"
    (let [{user-id :users/id} (factories/create-user)
          {:organizations/keys [id]} (factories/create-organization user-id)]
      (is (nil? (:organizations/join-secret (organization/by-id id)))))))

(deftest enable-join-requests-sets-join-secret-test
  (testing "Enabling join requests on organization sets the join secret"
    (let [{user-id :users/id} (factories/create-user)
          {:organizations/keys [id]} (factories/create-organization user-id)]
      (organization/enable-join-requests! id)

      (is (= 27 (count (:organizations/join-secret (organization/by-id id))))))))

(deftest disable-join-requests-removes-the-join-secret-test
  (testing "Disabling join requests on organization removes the join secret"
    (let [{user-id :users/id} (factories/create-user)
          {:organizations/keys [id]} (factories/create-organization user-id)]

      (organization/enable-join-requests! id)
      (is (= 27 (count (:organizations/join-secret (organization/by-id id)))))
      (organization/disable-join-requests! id)
      (is (nil? (:organizations/join-secret (organization/by-id id)))))))
