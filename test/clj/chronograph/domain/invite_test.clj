(ns chronograph.domain.invite-test
  (:require [clojure.test :refer :all]
            [chronograph.domain.invite :as invite]
            [chronograph.db.invite :as db-invite]
            [chronograph.db.core :as db]
            [chronograph.factories :as factories]
            [chronograph.fixtures :as fixtures]
            [chronograph.test-utils :as tu]
            [chronograph.domain.acl :as acl]))

(use-fixtures :once fixtures/config fixtures/datasource)
(use-fixtures :each fixtures/clear-db)

(deftest find-or-create-test
  (testing "Given a org id and an email, it creates an invite"
    (let [{user-id :users/id} (factories/create-user)
          {organization-id :organizations/id} (factories/create-organization user-id)
          invite (invite/find-or-create! db/datasource organization-id "test@email.com")]
      (is (= #:invites{:organization-id organization-id,
                       :email           "test@email.com"}
             (dissoc invite :invites/id)))
      (is (= invite
             (db-invite/find-by-id db/datasource (:invites/id invite))))))
  (testing "Given an org id and email, it returns the invite if it already exists"
    (tu/with-fixtures [fixtures/clear-db]
      (let [{user-id :users/id} (factories/create-user)
            {organization-id :organizations/id} (factories/create-organization user-id)
            invite (invite/find-or-create! db/datasource organization-id "test@email.com")]
        (is (= invite
               (invite/find-or-create! db/datasource organization-id "test@email.com")))
        (is (= [invite]
               (db-invite/find-by-org-id db/datasource organization-id)))))))

(deftest find-by-org-id-test
  (testing "Can retrieve all created invites given an org ID"
    (let [{user-id :users/id} (factories/create-user)
          {organization-id :organizations/id} (factories/create-organization user-id)
          invite1 (invite/find-or-create! db/datasource organization-id "test1@email.com")
          invite2 (invite/find-or-create! db/datasource organization-id "test2@email.com")
          invite3 (invite/find-or-create! db/datasource organization-id "test3@email.com")
          {organization-id2 :organizations/id} (factories/create-organization user-id)
          invite4 (invite/find-or-create! db/datasource organization-id2 "test4@email.com")]
      (is (= #{invite1 invite2 invite3}
             (set (invite/find-by-org-id db/datasource organization-id))))
      (is (= #{invite4}
             (set (invite/find-by-org-id db/datasource organization-id2)))))))

(deftest find-by-org-slug-and-email-test
  (testing "Can retrieve an invite given an org slug and email"
    (let [{user-id :users/id} (factories/create-user)
          {organization-id :organizations/id
           slug1           :organizations/slug} (factories/create-organization user-id)
          {organization-id2 :organizations/id
           slug2            :organizations/slug} (factories/create-organization user-id)
          {slug3 :organizations/slug} (factories/create-organization user-id)
          invite1 (invite/find-or-create! db/datasource organization-id "test1@email.com")
          invite2 (invite/find-or-create! db/datasource organization-id2 "test1@email.com")]
      (is (= invite1
             (invite/find-by-org-slug-and-email db/datasource slug1 "test1@email.com")))
      (is (= invite2
             (invite/find-by-org-slug-and-email db/datasource slug2 "test1@email.com")))
      (is (nil? (invite/find-by-org-slug-and-email db/datasource slug3 "test1@email.com"))
          "Should return nil if the invite doesn't exist"))))

(deftest accept!-test
  (testing "Can accept an invitation for a user and invite id"
    (let [{user-id :users/id} (factories/create-user)
          {organization-id :organizations/id
           org-slug :organizations/slug} (factories/create-organization user-id)
          {user-id-2 :users/id
           email     :users/email} (factories/create-user)
          {:invites/keys [id] :as created-invite} (invite/find-or-create! db/datasource organization-id email)
          accepted-invite (invite/accept! db/datasource id user-id-2)]
      (is (acl/member? db/datasource user-id-2 organization-id)
          "The user should be a member of the organization")
      (is (nil? (invite/find-by-org-slug-and-email db/datasource org-slug email))
          "The invite should be removed")
      (is (= created-invite accepted-invite)
          "The accepted invite should be returned"))))

(deftest reject!-test
  (testing "Can reject a given invite ID"
    (let [{user-id :users/id} (factories/create-user)
          {organization-id :organizations/id
           org-slug :organizations/slug} (factories/create-organization user-id)
          {user-id-2 :users/id
           email     :users/email} (factories/create-user)
          {:invites/keys [id] :as created-invite} (invite/find-or-create! db/datasource organization-id email)
          rejected-invite (invite/reject! db/datasource id)]
      (is (not (acl/belongs-to-org? db/datasource user-id-2 organization-id))
          "The user should NOT belong to the organization")
      (is (nil? (invite/find-by-org-slug-and-email db/datasource org-slug email))
          "The invite should be removed")
      (is (= created-invite rejected-invite)
          "The rejected invite should be returned"))))
