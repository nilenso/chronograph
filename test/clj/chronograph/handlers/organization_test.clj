(ns chronograph.handlers.organization-test
  (:require [chronograph.domain.acl :as acl]
            [chronograph.factories :as factories]
            [chronograph.fixtures :as fixtures]
            [chronograph.handlers.organization :as organization]
            [clojure.spec.alpha :as s]
            [clojure.test :refer :all]
            [chronograph.db.core :as db]
            [chronograph.domain.invite :as invite]
            [chronograph.test-utils :as tu])
  (:import (org.postgresql.util PSQLException)))

(use-fixtures :once  fixtures/config fixtures/datasource)
(use-fixtures :each fixtures/clear-db)

(deftest create-new-organization-first-time
  (testing "Creating a new organization returns a valid organization map in the response, and the creator is registered as admin in the ACL."
    (let [user (factories/create-user)
          response (organization/create {:body {:name "foo" :slug "bar"}
                                         :user user})]
      (is (= 200 (:status response)))
      (is (s/valid? :organizations/organization
                    (:body response)))
      (is (acl/admin? db/datasource
                      (:users/id user)
                      (:organizations/id (:body response)))))))

(deftest create-organization-when-slug-exists
  (testing "Creating an organization with pre-existing slug throws an exception."
    (let [user (factories/create-user)]
      (organization/create {:body {:name "foo" :slug "bar"}
                            :user user})
      (is (thrown? PSQLException (organization/create {:body {:name "foo" :slug "bar"}
                                                       :user user}))))))

(deftest create-organization-disallows-bad-request-params
  (testing "Creating an organization with non-conforming name and/or slug fails with HTTP error."
    (let [user (factories/create-user)]
      (is (= {:status 400
              :headers {}
              :body {:error "Bad name or slug."}}
             (organization/create {:body {:name "" :slug "foo"}
                                   :user user}))
          "Name cannot be empty.")
      (is (= {:status 400
              :headers {}
              :body {:error "Bad name or slug."}}
             (organization/create {:body {:name "foo bar" :slug ""}
                                   :user user}))
          "Slug cannot be empty.")
      (is (= {:status 400
              :headers {}
              :body {:error "Bad name or slug."}}
             (organization/create {:body {:name "foo bar" :slug "abc - 123 "}
                                   :user user}))
          "Slug must contain only lowercase letters, and optionally numbers or hypens. No whitespace allowed.")
      (is (= {:status 400
              :headers {}
              :body {:error "Bad name or slug."}}
             (organization/create
              {:body {:name "foo bar"
                      :slug (->> (repeat 257 "a")
                                 (apply str))}
               :user user}))
          "Slug length must not exceed 256 characters.")
      (let [response (organization/create
                      {:body {:name "foo bar"
                              :slug (->> (repeat (/ 256 4) "a-34")
                                         (apply str))}
                       :user user})]
        (is (= 200 (:status response)))
        (is (s/valid? :organizations/organization
                      (:body response))
            "Slug length can be exactly 256 characters at most.")))))

(deftest find-one-organization
  (testing "when fetching an organization"
    (let [user (factories/create-user)
          {:organizations/keys [slug]
           :as organization} (factories/create-organization (:users/id user))
          other-user (factories/create-user)
          slug-that-doesnt-exist (str (java.util.UUID/randomUUID))]
      (is (= {:status 200
              :headers {}
              :body organization}
             (organization/find-one {:params {:slug slug}
                                     :user user}))
          "Fetching an organization with authorized user returns the organization details.")
      (is (= {:status 404
              :headers {}
              :body {:error "Not found"}}
             (organization/find-one {:params {:slug slug}
                                     :user other-user}))
          "Fetching an organization with unauthorized user fails with HTTP error.")
      (is (= {:status 400
              :headers {}
              :body {:error "Bad slug"}}
             (organization/find-one {:params {:slug nil}
                                     :user user}))
          "Fetching an organization with invalid slug fails with HTTP error.")
      (is (= {:status 404
              :headers {}
              :body {:error "Not found"}}
             (organization/find-one {:params {:slug slug-that-doesnt-exist}
                                     :user user}))
          "Fetching a non-existing organization fails with HTTP error.")
      (is (= {:status 404
              :headers {}
              :body {:error "Not found"}}
             (do ;; create org so the "other-user" gets registered in the ACL
               (factories/create-organization (:users/id other-user))
               ;; now try to find an org to which "other-user" does not belong
               (organization/find-one {:params {:slug slug}
                                       :user other-user})))
          "Fetching org details by a user that does not belong to the org fails with HTTP error."))))

(deftest show-members-test
  (testing "Should fetch joined and invited members and return a 200"
    (let [user         (factories/create-user)
          {:organizations/keys [slug]
           org-id              :organizations/id} (factories/create-organization (:users/id user))
          joined-user  (factories/create-user)
          invited-user (invite/find-or-create! db/datasource org-id "invite@user.com")]
      (acl/create! db/datasource {:user-id         (:users/id joined-user)
                                  :organization-id org-id
                                  :role            acl/member})
      (is (= {:status 200
              :body   {:invited [invited-user]
                       :joined  [user joined-user]}}
             (-> (organization/show-members {:params {:slug slug}
                                             :user   user})
                 (select-keys [:status :body]))))))

  (testing "Should return a 404 if no org is found with the given slug"
    (tu/with-fixtures [fixtures/clear-db]
      (let [user (factories/create-user)]
        (is (= {:status 404
                :body   {:error "Not found"}}
               (-> (organization/show-members {:params {:slug "foobar"}
                                               :user   user})
                   (select-keys [:status :body])))))))

  (testing "Should return a 403 if the authenticated user is not an admin of the org"
    (tu/with-fixtures [fixtures/clear-db]
      (let [user (factories/create-user)
            {:organizations/keys [slug]
             org-id              :organizations/id} (factories/create-organization (:users/id user))
            member  (factories/create-user)]
        (acl/create! db/datasource {:user-id         (:users/id member)
                                    :organization-id org-id
                                    :role            acl/member})
        (is (= {:status 403
                :body   {:error "Forbidden"}}
               (-> (organization/show-members {:params {:slug slug}
                                               :user   member})
                   (select-keys [:status :body]))))))))

(deftest invite-test
  (testing "When the organization exists and user is an admin, an invite should be created"
    (let [{user-id :users/id} (factories/create-user)
          {:organizations/keys [slug]
           organization-id :organizations/id} (factories/create-organization user-id)
          response (organization/invite {:params {:slug slug}
                                         :user {:users/id user-id}
                                         :body {:email "test@email.com"}})]
      (is (= {:status 200
              :body (first (invite/find-by-org-id db/datasource organization-id))}
             (select-keys response [:status :body])))))

  (testing "Should return 404 if org is not found with given slug"
    (tu/with-fixtures [fixtures/clear-db]
      (let [{user-id :users/id} (factories/create-user)]
        (is (= {:status 404
                :body {:error "Not found"}}
               (-> (organization/invite {:params {:slug "foobar"}
                                         :user {:users/id user-id}
                                         :body {:email "test@email.com"}})
                   (select-keys [:status :body])))))))

  (testing "Should return 403 if user is not an admin of the org"
    (tu/with-fixtures [fixtures/clear-db]
      (let [{user-id :users/id} (factories/create-user)
            {organization-id :organizations/id
             slug :organizations/slug} (factories/create-organization user-id)
            {user-id-2 :users/id} (factories/create-user)]
        (acl/create! db/datasource {:user-id user-id-2
                                    :organization-id organization-id
                                    :role acl/member})
        (is (= {:status 403
                :body {:error "Forbidden"}}
               (-> (organization/invite {:params {:slug slug}
                                         :user {:users/id user-id-2}
                                         :body {:email "test@email.com"}})
                   (select-keys [:status :body])))))))

  (testing "Should return 400 if the email is invalid"
    (tu/with-fixtures [fixtures/clear-db]
      (let [{user-id :users/id :as user} (factories/create-user)
            {:organizations/keys [slug]} (factories/create-organization user-id)]
        (is (= {:status 400
                :body   {:error "Invalid email"}}
               (-> (organization/invite {:params {:slug slug}
                                         :user   user
                                         :body   {:email "test@emailcom"}})
                   (select-keys [:status :body]))))))))
