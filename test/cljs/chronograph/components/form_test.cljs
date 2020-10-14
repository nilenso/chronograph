(ns chronograph.components.form-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests]]
            [day8.re-frame.test :as rf-test]
            [chronograph-web.components.form :as form]
            [chronograph.test-utils :as tu]))

(defn- event-with-value
  [v]
  (clj->js {:currentTarget {:value v}}))

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
       (let [{::form/keys [input-attributes-builder]} (form/form {:form-key        ::form-key
                                                                  :initial-values {:foobar "baz"}
                                                                  :request-builder (constantly {})})]
         (is (= {:value       "baz"
                 :placeholder "Foobar"}
                (dissoc (input-attributes-builder :foobar) :on-change))
             "The value of the input should be set to its initial value"))))

    (testing "When only an input key is passed"
      (rf-test/run-test-sync
       (let [{::form/keys [input-attributes-builder]} (form/form {:form-key        ::form-key
                                                                  :request-builder (constantly {})})
             {:keys [on-change]} (input-attributes-builder :foobar)]
         (on-change (event-with-value "new-value"))
         (is (= {:value       "new-value"
                 :placeholder "Foobar"}
                (dissoc (input-attributes-builder :foobar) :on-change))
             "The value of the input should change after calling on-change"))))

    (testing "When additional attributes are passed"
      (rf-test/run-test-sync
       (let [{::form/keys [input-attributes-builder]} (form/form {:form-key        ::form-key
                                                                  :request-builder (constantly {})})]
         (is (= {:value       nil
                 :placeholder "Foobar"
                 :baz         "quux"
                 :class       "bar"}
                (dissoc (input-attributes-builder :foobar {:baz   "quux"
                                                           :class "bar"}) :on-change))
             "The supplied attributes should be present in the returned attributes"))))

    (testing "When additional attributes and a spec are passed"
      (testing "When the spec fails"
        (rf-test/run-test-sync
         (let [{::form/keys [input-attributes-builder]} (form/form {:form-key        ::form-key
                                                                    :request-builder (constantly {})})]
           (is (= {:value       nil
                   :placeholder "Foobar"
                   :baz         "quux"
                   :class       "bar form-error"}
                  (dissoc (input-attributes-builder :foobar
                                                    {:baz   "quux"
                                                     :value "value"
                                                     :class "bar"}
                                                    int?) :on-change))
               "The form-error class should be set")))))))

(deftest form-submission-test
  (testing "The built request should be supplied to the http-xhrio effect"
    (rf-test/run-test-sync
     (let [xhrio-effect (tu/stub-xhrio {} true)
           {::form/keys [submit-attributes
                         input-attributes-builder]} (form/form {:form-key        ::form-key
                                                                :request-builder (fn [form-params]
                                                                                   {:uri        "/api/foo"
                                                                                    :method     :post
                                                                                    :params     form-params
                                                                                    :on-success [::foo-event "bar"]
                                                                                    :on-failure [::baz-event "quux"]})})
           {:keys [on-change]} (input-attributes-builder :foobar)
           {:keys [on-click]} submit-attributes]
       (on-change (event-with-value "new-value"))
       (on-click nil)
       (is (= {:uri        "/api/foo"
               :method     :post
               :params     {:foobar "new-value"}
               :on-success [::form/submit-form-success ::form-key [::foo-event "bar"]]
               :on-failure [::form/submit-form-failure ::form-key [::baz-event "quux"]]}
              @xhrio-effect)))))

  (testing "When form submission succeeds"
    (rf-test/run-test-sync
     (let [dispatched-event (tu/stub-event ::foo-event)
           {::form/keys [submit-attributes
                         input-attributes-builder]} (form/form {:form-key        ::form-key
                                                                :request-builder (constantly
                                                                                  {:on-success [::foo-event "bar"]})})
           {:keys [on-change]} (input-attributes-builder :foobar)
           {:keys [on-click]} submit-attributes]
       (on-change (event-with-value "new-value"))
       (tu/stub-xhrio {:fake "response"} true)
       (on-click nil)
       (is (= [::foo-event "bar" {:fake "response"}]
              @dispatched-event)
           "The on-success event should be dispatched with the received response"))))

  (testing "When form submission fails"
    (rf-test/run-test-sync
     (let [dispatched-event (tu/stub-event ::baz-event)
           {::form/keys [submit-attributes
                         input-attributes-builder]} (form/form {:form-key        ::form-key
                                                                :request-builder (constantly
                                                                                  {:on-failure [::baz-event "quux"]})})
           {:keys [on-change]} (input-attributes-builder :foobar)
           {:keys [on-click]} submit-attributes]
       (on-change (event-with-value "new-value"))
       (tu/stub-xhrio {:fake "response"} false)
       (on-click nil)
       (is (= [::baz-event "quux" {:fake "response"}]
              @dispatched-event)
           "The on-failure event should be dispatched with the received response")))))

