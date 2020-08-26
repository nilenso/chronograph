(ns chronograph.handlers.google-auth-test
  (:require [clojure.test :refer :all]
            [chronograph.handlers.google-auth :as google-auth]
            [mock-clj.core :as mc]
            [org.httpkit.client :as http]
            [cheshire.core :as json]
            [chronograph.fixtures :as fixtures]
            [chronograph.auth :as auth]))

(use-fixtures :once fixtures/config)

(deftest oauth2-redirect-handler-test
  (testing "when called with a code and no error"
    (mc/with-mock [http/post                      (delay {:body (-> {"id_token" "fake-token"}
                                                                    (json/generate-string))})
                   google-auth/token->credentials {"name"           "foobar"
                                                   "sub"            "123456"
                                                   "email"          "foo@bar.com"
                                                   "email_verified" true}]
      (let [{:keys [status cookies]} (google-auth/oauth2-redirect-handler {:params {:code "123"}})]
        (is (= 302 status))
        (is (= {:id       "123456"
                :name     "foobar"
                :email    "foo@bar.com"
                :provider "google"}
               (-> cookies
                   (get "auth-token")
                   :value
                   auth/unsign-token
                   (dissoc :exp))))))))
