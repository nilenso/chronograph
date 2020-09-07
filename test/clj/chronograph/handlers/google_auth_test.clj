(ns chronograph.handlers.google-auth-test
  (:require [clojure.test :refer :all]
            [chronograph.handlers.google-auth :as google-auth]
            [mock-clj.core :as mc]
            [org.httpkit.client :as http]
            [cheshire.core :as json]
            [chronograph.fixtures :as fixtures]
            [chronograph.auth :as auth]
            [chronograph.db.user :as users-db]))

(use-fixtures :once fixtures/config fixtures/datasource)
(use-fixtures :each fixtures/clear-db)

(deftest web-redirect-handler-test
  (testing "when called with a code and no error"
    (mc/with-mock [http/post                   (delay {:status 200
                                                       :body   (-> {"id_token" "fake-token"}
                                                                   (json/generate-string))})
                   google-auth/verify-id-token {"name"           "foobar"
                                                "sub"            "123456"
                                                "email"          "foo@bar.com"
                                                "email_verified" true
                                                "picture"        "https://foo/bar.png"}]
      (let [{:keys [status cookies]} (google-auth/web-redirect-handler {:params {:code "123"}})
            {:user/keys [id]} (users-db/find-by-google-id "123456")]
        (is (= 302 status))
        (is (= id
               (-> cookies
                   (get "auth-token")
                   :value
                   auth/verify-token
                   :id))))))

  (testing "when called with an error"
    (let [{:keys [status headers]} (google-auth/web-redirect-handler {:params {:error "google-failed"}})]
      (is (= 302 status))
      (is (= "/?auth-error=google-failed"
             (get headers "Location")))))

  (testing "when the email from the id_token is not verified"
    (mc/with-mock [http/post                   (delay {:status 200
                                                       :body   (-> {"id_token" "fake-token"}
                                                                   (json/generate-string))})
                   google-auth/verify-id-token {"name"           "foobar"
                                                "sub"            "123456"
                                                "email"          "foo@bar.com"
                                                "email_verified" false
                                                "picture"        "https://foo/bar.png"}]
      (let [{:keys [status headers]} (google-auth/web-redirect-handler {:params {:code "123"}})]
        (is (= 302 status))
        (is (= "/?auth-error=email-not-verified"
               (get headers "Location"))))))

  (testing "when the token endpoint returns a non-200 status code"
    (mc/with-mock [http/post (delay {:status 500
                                     :body   (-> {"error" "big problem"}
                                                 (json/generate-string))})]
      (let [{:keys [status headers]} (google-auth/web-redirect-handler {:params {:code "123"}})]
        (is (= 302 status))
        (is (= "/?auth-error=unexpected-error"
               (get headers "Location"))))))

  (testing "when the call to the token endpoint throws an exception"
    (mc/with-mock [http/post (delay {:error (ex-info "Timeout!" {:time :out})})]
      (let [{:keys [status headers]} (google-auth/web-redirect-handler {:params {:code "123"}})]
        (is (= 302 status))
        (is (= "/?auth-error=unexpected-error"
               (get headers "Location")))))))
