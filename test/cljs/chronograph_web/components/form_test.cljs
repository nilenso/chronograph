(ns chronograph-web.components.form-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests use-fixtures]]
            [chronograph-web.components.form :as form]
            [chronograph-web.test-utils :as tu]
            [re-frame.core :as rf]
            [chronograph-web.fixtures :as fixtures]))

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

(deftest set-values-test
  (tu/rf-test "It should set the form's values"
    (let [{::form/keys [get-input-attributes]} (form/form {:form-key        ::form-key
                                                           :initial-values  {:foobar "baz"}
                                                           :request-builder (constantly {})})]
      (rf/dispatch [::form/set-values ::form-key {:foobar "quux"}])
      (is (= "quux"
             (:value (get-input-attributes :foobar)))
          "The value of the input should be changed"))))

(deftest form-attributes-test
  (testing "Input attributes builder"
    (tu/rf-test "When initial values are passed"
      (let [{::form/keys [get-input-attributes]} (form/form {:form-key        ::form-key
                                                             :initial-values  {:foobar "baz"}
                                                             :request-builder (constantly {})})]
        (is (= "baz"
               (:value (get-input-attributes :foobar)))
            "The value of the input should be set to its initial value")))

    (tu/rf-test "When only an input key is passed"
      (let [{::form/keys [get-input-attributes]} (form/form {:form-key        ::form-key
                                                             :request-builder (constantly {})})
            {:keys [onChange]} (get-input-attributes :foobar)]
        (onChange (event-with-value "new-value"))
        (is (= {:value       "new-value"
                :placeholder "Foobar"}
               (select-keys (get-input-attributes :foobar) [:value :placeholder]))
            "The value of the input should change after calling onChange")))

    (testing "When a spec is passed"
      (tu/rf-test "When the spec fails"
        (let [{::form/keys [get-input-attributes]} (form/form {:form-key        ::form-key
                                                               :specs           {:foobar int?}
                                                               :request-builder (constantly {})})]
          (is (= {:value       nil
                  :placeholder "Foobar"
                  :class       "form-error"}
                 (select-keys (get-input-attributes :foobar
                                                    {:spec int?})
                              [:value :placeholder :class]))
              "The form-error class should be set")))))

  (testing "Submit attributes builder"
    (tu/rf-test "Button should be disabled if there is a spec error"
      (let [{::form/keys [get-submit-attributes]} (form/form {:form-key        ::form-key
                                                              :specs           {:foobar int?}
                                                              :initial-values  {:foobar "not-an-int"}
                                                              :request-builder (constantly {})})]
        (is (:disabled (get-submit-attributes)))))

    (tu/rf-test "Button should be set to loading while submitting"
      (let [{::form/keys [get-submit-attributes]} (form/form {:form-key        ::form-key
                                                              :request-builder (constantly {})})
            {:keys [onClick]} (get-submit-attributes)]
        (rf/reg-fx :http-xhrio (constantly nil))
        (onClick fake-event)
        (is (:loading (get-submit-attributes)))))))

(deftest form-submission-test
  (tu/rf-test "The built request should be supplied to the http-xhrio effect"
    (let [xhrio-effect (tu/stub-xhrio {} true)
          {::form/keys [get-submit-attributes
                        get-input-attributes]} (form/form {:form-key        ::form-key
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
             @xhrio-effect))))

  (tu/rf-test "When form submission succeeds"
    (let [dispatched-event (tu/stub-event ::foo-event)
          {::form/keys [get-submit-attributes
                        get-input-attributes]} (form/form {:form-key        ::form-key
                                                           :request-builder (constantly
                                                                             {:on-success [::foo-event "bar"]})})
          {:keys [onChange]} (get-input-attributes :foobar)
          {:keys [onClick]} (get-submit-attributes)]
      (onChange (event-with-value "new-value"))
      (tu/stub-xhrio {:fake "response"} true)
      (onClick fake-event)
      (is (= [::foo-event "bar" {:fake "response"}]
             @dispatched-event)
          "The on-success event should be dispatched with the received response")))

  (tu/rf-test "When form submission fails"
    (let [dispatched-event (tu/stub-event ::baz-event)
          {::form/keys [get-submit-attributes
                        get-input-attributes]} (form/form {:form-key        ::form-key
                                                           :request-builder (constantly
                                                                             {:on-failure [::baz-event "quux"]})})
          {:keys [onChange]} (get-input-attributes :foobar)
          {:keys [onClick]} (get-submit-attributes)]
      (onChange (event-with-value "new-value"))
      (tu/stub-xhrio {:fake "response"} false)
      (onClick fake-event)
      (is (= [::baz-event "quux" {:fake "response"}]
             @dispatched-event)
          "The on-failure event should be dispatched with the received response"))))
