(ns chronograph.handlers.invite-test
  (:require [clojure.test :refer :all]
            [chronograph.factories :as factories]
            [chronograph.handlers.invite :as invite-handlers]
            [chronograph.db.core :as db]
            [chronograph.domain.invite :as invite]
            [chronograph.domain.acl :as acl]
            [chronograph.fixtures :as fixtures]
            [chronograph.test-utils :as tu]))

(use-fixtures :once fixtures/config fixtures/datasource)
(use-fixtures :each fixtures/clear-db)

(deftest invited-test
  (testing "Returns a 200 with a list of invited organizations"
    (let [{user-id-1 :users/id} (factories/create-user)
          {org-id :organizations/id :as org1} (factories/create-organization user-id-1)
          {email :users/email} (factories/create-user)]
      (invite/find-or-create! db/datasource org-id email)
      (is (= {:status 200
              :body   [org1]}
             (-> (invite-handlers/invited-orgs {:user #:users{:email email}})
                 (select-keys [:status :body])))))))

(deftest accept-test
  (tu/with-fixtures [fixtures/clear-db]
    (testing "If the invite exists"
      (let [{organization-id :organizations/id
             org-slug        :organizations/slug} (factories/create-user-and-organization)
            {user-id :users/id
             email   :users/email
             :as     user} (factories/create-user)
            created-invite (invite/find-or-create! db/datasource organization-id email)]
        (is (= {:status 200
                :body   created-invite}
               (-> (invite-handlers/accept {:user   user
                                            :params {:slug org-slug}})
                   (select-keys [:status :body]))))
        (is (acl/member? db/datasource user-id organization-id)))))

  (tu/with-fixtures [fixtures/clear-db]
    (testing "If the invite doesn't exist"
      (let [{organization-id :organizations/id
             org-slug        :organizations/slug} (factories/create-user-and-organization)
            {user-id :users/id :as user} (factories/create-user)]
        (is (= {:status 404
                :body   {:error "Not found"}}
               (-> (invite-handlers/accept {:user   user
                                            :params {:slug org-slug}})
                   (select-keys [:status :body]))))
        (is (not (acl/member? db/datasource user-id organization-id)))))))

(deftest reject-test
  (tu/with-fixtures [fixtures/clear-db]
    (testing "If the invite exists"
      (let [{organization-id :organizations/id
             org-slug        :organizations/slug} (factories/create-user-and-organization)
            {user-id :users/id
             email   :users/email
             :as     user} (factories/create-user)
            created-invite (invite/find-or-create! db/datasource organization-id email)]
        (is (= {:status 200
                :body   created-invite}
               (-> (invite-handlers/reject {:user   user
                                            :params {:slug org-slug}})
                   (select-keys [:status :body]))))
        (is (not (acl/member? db/datasource user-id organization-id))))))

  (tu/with-fixtures [fixtures/clear-db]
    (testing "If the invite doesn't exist"
      (let [{organization-id :organizations/id
             org-slug        :organizations/slug} (factories/create-user-and-organization)
            {user-id :users/id :as user} (factories/create-user)]
        (is (= {:status 404
                :body   {:error "Not found"}}
               (-> (invite-handlers/reject {:user   user
                                            :params {:slug org-slug}})
                   (select-keys [:status :body]))))
        (is (not (acl/member? db/datasource user-id organization-id)))))))
