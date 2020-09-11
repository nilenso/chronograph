(ns chronograph.handlers.organization-test
  (:require [chronograph.domain.acl :as acl]
            [chronograph.factories :as factories]
            [chronograph.fixtures :as fixtures]
            [chronograph.handlers.organization :as organization]
            [clojure.spec.alpha :as s]
            [clojure.test :refer :all])
  (:import (org.postgresql.util PSQLException)))

(use-fixtures :once fixtures/config fixtures/datasource)
(use-fixtures :each fixtures/clear-db)

(deftest create-new-organization-first-time
  (testing "Creating a new organization returns a valid organization map in the response, and the creator is registered as admin in the ACL."
    (let [user (factories/create-user)
          response (organization/create {:body {:name "foo" :slug "bar"}
                                         :user user})]
      (is (= 200 (:status response)))
      (is (s/valid? :organizations/organization
                    (:body response)))
      (is (acl/admin? (:users/id user)
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

<<<<<<< HEAD
(deftest find-one-organization
  (testing "when fetching an organization"
    (let [user (factories/create-user)
          {:organizations/keys [slug]
           :as organization} (factories/create-organization (:users/id user))
          other-user (factories/create-user)
          slug-that-doesnt-exist (str (java.util.UUID/randomUUID))]
=======

(deftest find-one-organization
  (testing "when fetching an organization"
    (let [user (factories/create-user)
          other-user (factories/create-user)
          slug "bar"
          slug-that-doesnt-exist (str (java.util.UUID/randomUUID))
          organization (:body (create-organization
                               {:body {:name "foo" :slug slug}
                                :user user}))]
>>>>>>> Add handler to find organization for an authorized
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
<<<<<<< HEAD
               (factories/create-organization (:users/id other-user))
=======
               (create-organization
                {:body {:name "other foo" :slug (str "other-" slug)}
                 :user other-user})
>>>>>>> Add handler to find organization for an authorized
               ;; now try to find an org to which "other-user" does not belong
               (organization/find-one {:params {:slug slug}
                                       :user other-user})))
          "Fetching org details by a user that does not belong to the org fails with HTTP error."))))
