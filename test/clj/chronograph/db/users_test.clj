(ns chronograph.db.users-test
  (:require [clojure.test :refer :all]
            [chronograph.db.users :as users-db]
            [chronograph.fixtures :as fixtures]))

(use-fixtures :once fixtures/config fixtures/datasource)
(use-fixtures :each fixtures/clear-db)

(deftest create-and-retrieve-google-user-test
  (testing "Can retrieve a user created by find-or-create"
    (let [created-user   (users-db/find-or-create-google-user! "google-123"
                                                               "Foo Bar"
                                                               "foo@bar.baz"
                                                               "https://foo.bar/baz.jpg")
          retrieved-user (users-db/find-by-google-id "google-123")]
      (is (= created-user retrieved-user))
      (is (= {:name      "Foo Bar"
              :email     "foo@bar.baz"
              :photo-url "https://foo.bar/baz.jpg"}
             (select-keys retrieved-user [:name :email :photo-url])))))

  (testing "find-or-create doesn't create a new user if they already exist"
    (let [created-user   (users-db/find-or-create-google-user! "google-456"
                                                               "Foo Bar"
                                                               "foo@bar.baz"
                                                               "https://foo.bar/baz.jpg")
          retrieved-user (users-db/find-or-create-google-user! "google-456"
                                                               "Foo Bar"
                                                               "foo@bar.baz"
                                                               "https://foo.bar/baz.jpg")]
      (is (= created-user retrieved-user))))

  (testing "find-by-google-id returns nil if the google-id is absent"
    (is (nil? (users-db/find-by-google-id "google-789")))))

(deftest find-by-id-test
  (testing "Can retrieve a user created by find-or-create"
    (let [{:keys [id] :as created-user} (users-db/find-or-create-google-user! "google-123"
                                                                              "Foo Bar"
                                                                              "foo@bar.baz"
                                                                              "https://foo.bar/baz.jpg")
          retrieved-user (users-db/find-by-id id)]
      (is (= created-user retrieved-user))
      (is (= {:name      "Foo Bar"
              :email     "foo@bar.baz"
              :photo-url "https://foo.bar/baz.jpg"}
             (select-keys retrieved-user [:name :email :photo-url]))))))
