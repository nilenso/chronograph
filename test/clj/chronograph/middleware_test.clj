(ns chronograph.middleware-test
  (:require [chronograph.auth :as auth]
            [chronograph.domain.acl :as acl]
            [chronograph.factories :as factories]
            [chronograph.fixtures :as fixtures]
            [chronograph.middleware :as middleware]
            [clojure.test :refer :all]
            [ring.util.response :as response]))

(use-fixtures :once fixtures/config fixtures/datasource fixtures/report-logging-only)
(use-fixtures :each fixtures/clear-db)

(deftest wrap-cookie-auth-test
  (let [user (factories/create-user)]
    (testing "Should return updated request having user's information, fetched from the DB, if the user is authentic."
      (is (= user
             (:user ((middleware/wrap-cookie-auth identity)
                     {:cookies {"auth-token" {:value (auth/create-token
                                                      (:users/id user))}}})))))

    (testing "Should return updated request with nil user, if the user is NOT authentic."
      (let [non-existent-user-id (+ (rand-int 100)
                                    1
                                    (:users/id user))]
        (is (nil?
             (:user ((middleware/wrap-cookie-auth identity)
                     {:cookies {"auth-token" {:value (auth/create-token non-existent-user-id)}}}))))))

    (testing "Should return updated request with nil user, if the auth-token has expired."
      (is (nil?
           (:user ((middleware/wrap-cookie-auth identity)
                   {:cookies {"auth-token" {:value (auth/create-token (:users/id user)
                                                                      -10)}}})))))))

(deftest wrap-header-auth-test
  (let [user (factories/create-user)]
    (testing "Should return updated request having user's information, fetched from the DB, if the user is authentic."
      (is (= user
             (:user ((middleware/wrap-header-auth identity)
                     {:headers {"authorization" (str "Bearer " (auth/create-token
                                                                (:users/id user)))}})))))

    (testing "Should return updated request with nil user, if the user is NOT authentic."
      (let [non-existent-user-id (+ (rand-int 100)
                                    1
                                    (:users/id user))]
        (is (nil?
             (:user ((middleware/wrap-header-auth identity)
                     {:headers {"authorization" (str "Bearer " (auth/create-token non-existent-user-id))}}))))))

    (testing "Should return updated request with nil user, if the auth-token has expired."
      (is (nil?
           (:user ((middleware/wrap-header-auth identity)
                   {:headers {"authorization" (str "Bearer " (auth/create-token (:users/id user)
                                                                                -10))}})))))))

(deftest wrap-require-user-info-test
  (let [fake-handler (constantly (-> (response/response "Success")
                                     (response/status 200)))]
    (testing "Calls the wrapped handler if the user key is present"
      (is (= {:status  200
              :headers {}
              :body    "Success"}
             ((middleware/wrap-require-user-info fake-handler)
              {:user "foobar"}))))

    (testing "Returns a 401 error if the user key is absent"
      (is (= 401
             (:status ((middleware/wrap-require-user-info fake-handler)
                       {})))))))

(deftest wrap-current-organization-test
  (let [fake-handler #(assoc {} :request %)
        user (factories/create-user)
        organization (factories/create-organization (:users/id user))]

    (testing "It adds the organization for the organization slug in params to the request"
      (let [request {:params {:slug (:organizations/slug organization)}}]
        (is (= organization
               (-> ((middleware/wrap-current-organization fake-handler) request)
                   :request
                   :organization)))))

    (testing "It does not add the organization to the request if no organization with the slug is present"
      (let [request {:params {:slug "unknown-organization"}}]
        (is (not (contains?
                  (-> ((middleware/wrap-current-organization fake-handler) request)
                      :request)
                  :organization)))))))

(deftest wrap-require-organization-test
  (let [fake-handler (constantly (-> (response/response "Success")
                                     (response/status 200)))
        user (factories/create-user)
        organization (factories/create-organization (:users/id user))]

    (testing "it calls the handler when organization is present in the request"
      (let [request {:organization organization}]
        (is (= {:status 200
                :headers {}
                :body "Success"}
               ((middleware/wrap-require-organization fake-handler) request)))))

    (testing "it returns 404 if the request does not have an organization"
      (let [request {:organization nil}]
        (is (= {:status 404
                :headers {}
                :body {:error "Organization not found"}}
               ((middleware/wrap-require-organization fake-handler) request)))))))

(deftest wrap-user-authorization-test
  (let [success (-> (response/response "Success")
                    (response/status 200))
        fake-handler (constantly success)
        admin (factories/create-user)
        member (factories/create-user)
        organization (factories/create-organization (:users/id admin))
        _member-acl (factories/create-acl member organization acl/member)]

    (testing "returns forbidden if the user does not have an acl for the organization"
      (let [other-user (factories/create-user)
            request {:organization organization
                     :user other-user}]
        (is (= 403
               (:status ((middleware/wrap-user-authorization
                          fake-handler
                          #{acl/admin acl/member})
                         request))))))

    (testing "returns forbidden if the user is a member and admin role is required"
      (let [request {:organization organization
                     :user member}]
        (is (= 403
               (:status ((middleware/wrap-user-authorization
                          fake-handler
                          #{acl/admin})
                         request))))))

    (testing "returns forbidden if the user is an admin and member role is required"
      (let [request {:organization organization :user admin}]
        (is (= 403
               (:status ((middleware/wrap-user-authorization
                          fake-handler
                          #{acl/member})
                         request))))))

    (testing "calls the handler if the user has the right role"
      (is (= success
             ((middleware/wrap-user-authorization
               fake-handler
               #{acl/member})
              {:organization organization :user member})))

      (is (= success
             ((middleware/wrap-user-authorization
               fake-handler
               #{acl/admin})
              {:organization organization :user admin})))

      (is (= success
             ((middleware/wrap-user-authorization
               fake-handler
               #{acl/admin acl/member})
              {:organization organization :user member})))

      (is (= success
             ((middleware/wrap-user-authorization
               fake-handler
               #{acl/admin acl/member})
              {:organization organization :user admin}))))))
