(ns chronograph.domain.user-test
  (:require [clojure.test :refer :all]
            [chronograph.fixtures :as fixtures]
            [chronograph.factories :as factories]
            [chronograph.domain.user :as user]
            [next.jdbc :refer [execute-one! with-transaction]]
            [chronograph.db.core :as db]
            [chronograph.utils.time :as time]))

(use-fixtures :once fixtures/config fixtures/datasource)
(use-fixtures :each fixtures/clear-db)

(deftest create-and-retrieve-google-user-test
  (testing "Can retrieve a user created by find-or-create"
    (with-redefs [time/now (constantly (time/now))]
      (let [created-user   (with-transaction [tx db/datasource]
                             (user/find-or-create-google-user! tx "google-123"
                                                               "Foo Bar"
                                                               "foo@bar.baz"
                                                               "https://foo.bar/baz.jpg"))
            retrieved-user (with-transaction [tx db/datasource]
                             (user/find-by-google-id tx "google-123"))]
        (is (= #:users{:id                 (:users/id created-user)
                       :google-profiles-id (:users/google-profiles-id created-user)
                       :name               "Foo Bar"
                       :email              "foo@bar.baz"
                       :photo-url          "https://foo.bar/baz.jpg"
                       :created-at         (time/now)
                       :updated-at         (time/now)}
               retrieved-user))))))

(deftest find-or-create-when-user-already-exists-test
  (testing "find-or-create doesn't create a new user if they already exist"
    (let [created-user   (with-transaction [tx db/datasource]
                           (user/find-or-create-google-user! tx "google-456"
                                                             "Foo Bar"
                                                             "foo@bar.baz"
                                                             "https://foo.bar/baz.jpg"))
          retrieved-user (with-transaction [tx db/datasource]
                           (user/find-or-create-google-user! tx "google-456"
                                                             "Foo Bar"
                                                             "foo@bar.baz"
                                                             "https://foo.bar/baz.jpg"))]
      (is (= created-user retrieved-user))
      (is (= 1
             (:count (execute-one! db/datasource ["select count(*) from users"])))))))

(deftest find-by-google-id-when-google-id-absent-test
  (testing "find-by-google-id returns nil if the google-id is absent"
    (is (nil? (with-transaction [tx db/datasource]
                (user/find-by-google-id tx "google-789"))))))

(deftest find-by-id-test
  (testing "Can retrieve a user created by find-or-create"
    (with-redefs [time/now (constantly (time/now))]
      (let [{:keys [users/id] :as created-user} (with-transaction [tx db/datasource]
                                                  (user/find-or-create-google-user! tx "google-123"
                                                                                    "Foo Bar"
                                                                                    "foo@bar.baz"
                                                                                    "https://foo.bar/baz.jpg"))
            retrieved-user (with-transaction [tx db/datasource]
                             (user/find-by-id tx id))]
        (is (= created-user retrieved-user))
        (is (= #:users{:id                 id
                       :google-profiles-id (:users/google-profiles-id created-user)
                       :name               "Foo Bar"
                       :email              "foo@bar.baz"
                       :photo-url          "https://foo.bar/baz.jpg"
                       :created-at         (time/now)
                       :updated-at         (time/now)}
               retrieved-user))))))

(deftest organizations-test
  (testing "Returns the list of organizations a user belongs to"
    (let [user (factories/create-user)
          organization1 (factories/create-organization (:users/id user))
          organization2 (factories/create-organization (:users/id user))]
      (with-transaction [tx db/datasource]
        (let [user-organizations (user/organizations tx user)]
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
        (let [user-organizations (user/organizations tx user)]
          (is (= 1 (count user-organizations)))
          (is (= #{(:organizations/id organization1)}
                 (set (map :organizations/id user-organizations)))))))))
