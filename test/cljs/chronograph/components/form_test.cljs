(ns chronograph.components.form-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests use-fixtures]]
            [day8.re-frame.test :as rf-test]
            [chronograph-web.components.form :as form]
            [chronograph.test-utils :as tu]
            [re-frame.core :as rf]
            [chronograph.fixtures :as fixtures]))

(use-fixtures :once fixtures/silence-logging)

(defn- event-with-value
  [v]
  (clj->js {:currentTarget {:value v}}))

(def fake-event (clj->js {:preventDefault (constantly nil)}))

(deftest sentence-case-test
  (testing "it returns hypenated strings in sentence case"
    (is (= "Sentence case" (form/sentence-case "sentence-case"))))

  (testing "it returns underscored strings in sentence case"
    (is (= "Sentence case" (form/sentence-case "sentence_case"))))

  (testing "it returns hypenated keywords in sentence case"
    (is (= "Sentence case" (form/sentence-case :sentence-case))))

  (testing "it returns underscored keywords in sentence case"
    (is (= "Sentence case" (form/sentence-case :sentence_case)))))

(deftest form-attributes-test
  (testing "Input attributes builder"
    (testing "When initial values are passed"
      (rf-test/run-test-sync
       (let [{::form/keys [get-input-attributes]} (form/form {:form-key            ::form-key
                                                              :initial-values  {:foobar "baz"}
                                                              :request-builder (constantly {})})]
         (is (= {:value       "baz"
                 :placeholder "Foobar"}
                (dissoc (get-input-attributes :foobar) :onChange))
             "The value of the input should be set to its initial value"))))

    (testing "When only an input key is passed"
      (rf-test/run-test-sync
       (let [{::form/keys [get-input-attributes]} (form/form {:form-key            ::form-key
                                                              :request-builder (constantly {})})
             {:keys [onChange]} (get-input-attributes :foobar)]
         (onChange (event-with-value "new-value"))
         (is (= {:value       "new-value"
                 :placeholder "Foobar"}
                (dissoc (get-input-attributes :foobar) :onChange))
             "The value of the input should change after calling onChange"))))

    (testing "When a spec is passed"
      (testing "When the spec fails"
        (rf-test/run-test-sync
         (let [{::form/keys [get-input-attributes]} (form/form {:form-key            ::form-key
                                                                :request-builder (constantly {})})]
           (is (= {:value       nil
                   :placeholder "Foobar"
                   :class       "form-error"}
                  (dissoc (get-input-attributes :foobar
                                                {:spec int?}) :onChange))
               "The form-error class should be set"))))))

  (testing "Submit attributes builder"
    (testing "Button should be disabled while submitting"
      (rf-test/run-test-sync
       (let [{::form/keys [get-submit-attributes]} (form/form {:form-key    ::form-key
                                                               :request-builder (constantly {})})
             {:keys [onClick]} (get-submit-attributes)]
         (rf/reg-fx :http-xhrio (constantly nil))
         (onClick fake-event)
         (is (:loading (get-submit-attributes))))))))

(deftest form-submission-test
  (testing "The built request should be supplied to the http-xhrio effect"
    (rf-test/run-test-sync
     (let [xhrio-effect (tu/stub-xhrio {} true)
           {::form/keys [get-submit-attributes
                         get-input-attributes]} (form/form {:form-key            ::form-key
                                                            :request-builder (fn [form-params]
                                                                               {:uri        "/api/foo"
                                                                                :method     :post
                                                                                :params     form-params
                                                                                :on-success [::foo-event "bar"]
                                                                                :on-failure [::baz-event "quux"]})})
           {:keys [onChange]} (get-input-attributes :foobar)
           {:keys [onClick]} (get-submit-attributes)]
       (onChange (event-with-value "new-value"))
       (onClick fake-event)
       (is (= {:uri        "/api/foo"
               :method     :post
               :params     {:foobar "new-value"}
               :on-success [::form/submit-form-success ::form-key [::foo-event "bar"]]
               :on-failure [::form/submit-form-failure ::form-key [::baz-event "quux"]]}
              @xhrio-effect)))))

  (testing "When form submission succeeds"
    (rf-test/run-test-sync
     (let [dispatched-event (tu/stub-event ::foo-event)
           {::form/keys [get-submit-attributes
                         get-input-attributes]} (form/form {:form-key            ::form-key
                                                            :request-builder (constantly
                                                                              {:on-success [::foo-event "bar"]})})
           {:keys [onChange]} (get-input-attributes :foobar)
           {:keys [onClick]} (get-submit-attributes)]
       (onChange (event-with-value "new-value"))
       (tu/stub-xhrio {:fake "response"} true)
       (onClick fake-event)
       (is (= [::foo-event "bar" {:fake "response"}]
              @dispatched-event)
           "The on-success event should be dispatched with the received response"))))

  (testing "When form submission fails"
    (rf-test/run-test-sync
     (let [dispatched-event (tu/stub-event ::baz-event)
           {::form/keys [get-submit-attributes
                         get-input-attributes]} (form/form {:form-key            ::form-key
                                                            :request-builder (constantly
                                                                              {:on-failure [::baz-event "quux"]})})
           {:keys [onChange]} (get-input-attributes :foobar)
           {:keys [onClick]} (get-submit-attributes)]
       (onChange (event-with-value "new-value"))
       (tu/stub-xhrio {:fake "response"} false)
       (onClick fake-event)
       (is (= [::baz-event "quux" {:fake "response"}]
              @dispatched-event)
           "The on-failure event should be dispatched with the received response")))))