(ns chronograph.domain.invite-test
  (:require [clojure.test :refer :all]
            [chronograph.domain.invite :as invite]
            [chronograph.db.invite :as db-invite]
            [chronograph.db.core :as db]
            [chronograph.factories :as factories]
            [chronograph.fixtures :as fixtures]))

(use-fixtures :once fixtures/config fixtures/datasource)
(use-fixtures :each fixtures/clear-db)

(deftest create-invite-test
  (testing "Given a slug and an email, it creates an invite"
    (let [{user-id :users/id} (factories/create-user)
          {organization-id :organizations/id
           slug            :organizations/slug} (factories/create-organization user-id)
          invite (invite/create! slug "test@email.com")]
      (is (= #:invites{:organization-id organization-id,
                       :email           "test@email.com"}
             (dissoc invite :invites/id)))
      (is (= invite
             (db-invite/find-by-id db/datasource (:invites/id invite)))))))

(deftest find-by-org-id-test
  (testing "Can retrieve all created invites given an org ID"
    (let [{user-id :users/id} (factories/create-user)
          {organization-id :organizations/id
           slug            :organizations/slug} (factories/create-organization user-id)
          invite1 (invite/create! slug "test1@email.com")
          invite2 (invite/create! slug "test2@email.com")
          invite3 (invite/create! slug "test3@email.com")
          {organization-id2 :organizations/id
           slug2            :organizations/slug} (factories/create-organization user-id)
          invite4 (invite/create! slug2 "test4@email.com")]
      (is (= #{invite1 invite2 invite3}
             (set (invite/find-by-org-id db/datasource organization-id))))
      (is (= #{invite4}
             (set (invite/find-by-org-id db/datasource organization-id2)))))))
