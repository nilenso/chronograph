(ns chronograph.domain.user-test
  (:require [clojure.test :refer :all]
            [chronograph.fixtures :as fixtures]
            [chronograph.domain.user :as user]
            [next.jdbc :as jdbc]
            [chronograph.db.core :as db]
            [chronograph.utils.time :as time]))

(use-fixtures :once fixtures/config fixtures/datasource)
(use-fixtures :each fixtures/clear-db)

(deftest create-and-retrieve-google-user-test
  (testing "Can retrieve a user created by find-or-create"
    (with-redefs [time/now (constantly (time/now))]
      (let [created-user   (user/find-or-create-google-user! "google-123"
                                                             "Foo Bar"
                                                             "foo@bar.baz"
                                                             "https://foo.bar/baz.jpg")
            retrieved-user (user/find-by-google-id "google-123")]
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
    (let [created-user   (user/find-or-create-google-user! "google-456"
                                                           "Foo Bar"
                                                           "foo@bar.baz"
                                                           "https://foo.bar/baz.jpg")
          retrieved-user (user/find-or-create-google-user! "google-456"
                                                           "Foo Bar"
                                                           "foo@bar.baz"
                                                           "https://foo.bar/baz.jpg")]
      (is (= created-user retrieved-user))
      (is (= 1
             (:count (jdbc/execute-one! db/datasource ["select count(*) from users"])))))))

(deftest find-by-google-id-when-google-id-absent-test
  (testing "find-by-google-id returns nil if the google-id is absent"
    (is (nil? (user/find-by-google-id "google-789")))))

(deftest find-by-id-test
  (testing "Can retrieve a user created by find-or-create"
    (with-redefs [time/now (constantly (time/now))]
      (let [{:keys [users/id] :as created-user} (user/find-or-create-google-user! "google-123"
                                                                                  "Foo Bar"
                                                                                  "foo@bar.baz"
                                                                                  "https://foo.bar/baz.jpg")
            retrieved-user (user/find-by-id id)]
        (is (= created-user retrieved-user))
        (is (= #:users{:id                 id
                       :google-profiles-id (:users/google-profiles-id created-user)
                       :name               "Foo Bar"
                       :email              "foo@bar.baz"
                       :photo-url          "https://foo.bar/baz.jpg"
                       :created-at         (time/now)
                       :updated-at         (time/now)}
               retrieved-user))))))
