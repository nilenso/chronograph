(ns chronograph.components.form-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests]]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.test.check.generators]
            [day8.re-frame.test :as rf-test]
            [re-frame.core :as rf]
            [chronograph-web.components.form :as form]))

(rf/reg-sub ::forms #(:forms %))

(s/def ::form-input-string string?)
(s/def ::form-input-int int?)
(s/def ::form-input-boolean boolean?)

(s/def ::form-nested-input
  (s/keys :req [::form-input-int ::form-input-boolean]
          :opt [::form-input-string]))

(s/def ::form-spec
  (s/keys :req-un [::form-input-string]
          :req [::form-nested-input]
          :opt-un [::form-input-boolean]))

(deftest initialize-test
  (testing "it adds the form-key to forms in the app db"
    (let [form-key   :new-form-key
          values     (gen/generate (s/gen ::form-spec))
          initial-db nil
          final-db   (form/initialize initial-db [::form/initialize form-key values])]
      (is (= {:status :initialized
              :params values}
             (get-in final-db [:forms form-key]))))))

(deftest update-input-test
  (testing "it updates the value of a form input in the app db"
    (let [form-key   :form-key
          input-key  :input-key
          values     (gen/generate (s/gen ::form-spec))
          new-value  (gen/generate (s/gen string?))
          initial-db (form/initialize nil [::form/initialize form-key values])
          final-db   (form/update-input initial-db [::form/update form-key input-key new-value])]
      (is (= new-value
             (get-in final-db [:forms form-key :params input-key]))))))

(deftest submit-form-test
  (testing "it sets the status of the form to submitting"
    (let [values     (gen/generate (s/gen ::form-spec))
          initial-db (form/initialize nil [::form/initialize :form-key values])
          {final-db :db} (form/submit-form {:db initial-db} [::form/submit-form :form-key (constantly {})])]
      (is (= :submitting
             (get-in final-db
                     [:forms :form-key :status])))))

  (testing "it returns an http request"
    (let [values     (gen/generate (s/gen ::form-spec))
          initial-db (form/initialize nil [::form/initialize :form-key values])
          {request :http-xhrio} (form/submit-form {:db initial-db} [::form/submit-form
                                                                    :form-key
                                                                    (fn [form-params]
                                                                      {:uri        "/submit/form"
                                                                       :method     :post
                                                                       :params     form-params
                                                                       :on-success [:foo "bar"]
                                                                       :on-failure [:baz "quux"]})])]
      (is (= {:uri        "/submit/form"
              :method     :post
              :params     values
              :on-success [::form/submit-form-success :form-key [:foo "bar"]]
              :on-failure [::form/submit-form-failure :form-key [:baz "quux"]]}
             (select-keys request
                          [:uri :method :params :on-success :on-failure]))))))

(deftest submit-form-success-test
  (testing "it removes the form-key from the app db"
    (let [form-key   :form-key
          values     (gen/generate (s/gen ::form-spec))
          initial-db (form/initialize nil [::form/initialize form-key values])
          final-db   (form/submit-form-success {:db initial-db} [::form/submit-form-success form-key nil])]
      (is (nil? (get-in final-db [:forms form-key]))))))

(deftest submit-form-failure-test
  (testing "it removes the form-key from the app db"
    (let [form-key   :form-key
          values     (gen/generate (s/gen ::form-spec))
          initial-db (form/initialize nil [::form/initialize form-key values])
          {final-db :db} (form/submit-form-failure {:db initial-db}
                                                   [::form/submit-form-success form-key nil])]
      (is (= :submit-failed
             (get-in final-db [:forms form-key :status])))
      (is (= values
             (get-in final-db [:forms form-key :params]))))))

(deftest form-subscripton-test
  (testing "it returns the form for the given form-key"
    (rf-test/run-test-sync
     (let [form-key      :form-key
           form          (rf/subscribe [::form/form form-key])
           initial-value (gen/generate (s/gen ::form-spec))]
       (rf/dispatch [::form/initialize form-key initial-value])
       (is (= initial-value (:params @form)))))))

(deftest sentence-case-test
  (testing "it returns hypenated strings in sentence case"
    (is (= "Sentence case" (form/sentence-case "sentence-case"))))

  (testing "it returns underscored strings in sentence case"
    (is (= "Sentence case" (form/sentence-case "sentence_case"))))

  (testing "it returns hypenated keywords in sentence case"
    (is (= "Sentence case" (form/sentence-case :sentence-case))))

  (testing "it returns underscored keywords in sentence case"
    (is (= "Sentence case" (form/sentence-case :sentence_case)))))

(deftest form-test
  (let [form-key  :form-key
        form-spec ::form-spec]
    (testing "it initialized the form the app db"
      (rf-test/run-test-sync
       (let [form (rf/subscribe [::form/form form-key])]
         (form/form {:form-key form-key :form-spec form-spec})
         (is (= {:status :initialized :params nil} @form)))))

    (testing "it initializes a form with initial values"
      (rf-test/run-test-sync
       (let [form          (rf/subscribe [::form/form form-key])
             initial-value (gen/generate (s/gen ::form-spec))]
         (form/form {:form-key form-key :form-spec form-spec :initial-values initial-value})
         (is (= {:status :initialized :params initial-value} @form)))))))

(deftest input-test
  (testing "it updates the input key in the form when the input changes"
    (rf-test/run-test-sync
     (let [new-input-value "input value"
           {::form/keys [input-attributes-builder]} (form/form {:form-key :form-key :form-spec ::form-spec})
           {:keys [on-change]} (input-attributes-builder :input-key ::form-input-string nil)
           form            (rf/subscribe [::form/form :form-key])]
       (on-change (clj->js {:currentTarget {:value new-input-value}}))
       (is (= new-input-value
              (get-in @form [:params :input-key]))))))

  (testing "it lets you set the on-change function"
    (rf-test/run-test-sync
     (let [{::form/keys [input-attributes-builder]} (form/form {:form-key :form-key :form-spec ::form-spec})
           {:keys [on-change]} (input-attributes-builder :input-key
                                                         ::form-input-string
                                                         {:on-change (constantly :custom-function)})
           new-input-value "input value"]
       (= :custom-function
          (on-change (clj->js {:currentTarget {:value new-input-value}}))))))

  (testing "it lets up set nested values for the input"
    (rf-test/run-test-sync
     (let [input-key       [:parent-key :input-key]
           {::form/keys [input-attributes-builder]} (form/form {:form-key :form-key})
           {:keys [on-change]} (input-attributes-builder input-key ::form-input-string nil)
           new-input-value "input value"
           form            (rf/subscribe [::form/form :form-key])]
       (on-change (clj->js {:currentTarget {:value new-input-value}}))
       (is (= new-input-value
              (get-in @form (flatten [:params input-key])))))))

  (testing "it sets the input value to the value in the db"
    (rf-test/run-test-sync
     (let [new-input-value "input value"
           {::form/keys [input-attributes-builder]} (form/form {:form-key :form-key :form-spec ::form-spec})
           form            (rf/subscribe [::form/form :form-key])
           _               (rf/dispatch [::form/update :form-key :input-key new-input-value])
           {:keys [value]} (input-attributes-builder :input-key ::form-input-string nil)]
       (is (= value
              (get-in @form [:params :input-key]))))))

  (testing "if the input value is invalid, it adds the 'form-error' class to the input"
    (rf-test/run-test-sync
     (let [new-input-value "input value"
           {::form/keys [input-attributes-builder]} (form/form {:form-key :form-key :form-spec ::form-spec})
           _               (rf/dispatch [::form/update :form-key :input-key new-input-value])
           {:keys [class]} (input-attributes-builder :input-key ::form-input-int nil)]
       (is (contains? (set class) "form-error"))))))

(deftest submit-test
  (testing "if the form params are invalid, it changes the status to invalid")
  (testing "if the form params are valid when clicked, it changes the status to submitting"
    (rf-test/run-test-sync
     (rf/reg-fx :http-xhrio
       (constantly nil))
     (let [form (rf/subscribe [::form/form :form-key])
           {::form/keys [submit-attributes]} (form/form {:form-key        :form-key
                                                         :request-builder (constantly {})})
           {:keys [on-click]} submit-attributes]
       (on-click nil)
       (is (= :submitting (:status @form))))))
  (testing "makes ajax request to submit the form using provided action and method"))
