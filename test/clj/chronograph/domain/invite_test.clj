(ns chronograph.domain.invite-test
  (:require [clojure.test :refer :all]
            [chronograph.domain.invite :as invite]
            [chronograph.db.invite :as db-invite]
            [chronograph.db.core :as db]
            [chronograph.factories :as factories]
            [chronograph.fixtures :as fixtures]
            [chronograph.test-utils :as tu]))

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
