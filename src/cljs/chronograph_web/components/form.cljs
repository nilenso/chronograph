(ns chronograph-web.components.form
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [chronograph-web.http :as http]
            [re-frame.core :as rf]))

(defn initialize [db [_ form-key initial-values]]
  (assoc-in db
            [:forms form-key]
            {:status :initialized
             :params initial-values}))

(rf/reg-event-db ::initialize initialize)

(defn update-input [db [_ form-key input-key value]]
  (-> db
      (assoc-in [:forms form-key :status] :editing)
      (assoc-in (flatten [:forms form-key :params input-key]) value)))

(rf/reg-event-db ::update update-input)

(defn submit-form
  [{:keys [db]} [_ form-key method uri on-success on-failure]]
  (let [params (get-in db [:forms form-key :params])]
    {:db (assoc-in db [:forms form-key :status] :submitting)
     :http-xhrio (http/request
                   {:uri uri
                    :method method
                    :params params
                    :on-success [::submit-form-success form-key on-success]
                    :on-failure [::submit-form-failure form-key on-failure]})}))

(rf/reg-event-fx ::submit-form submit-form)

;; TODO: Take arguments to tigger other effects
(defn submit-form-success [_ [_ form-key on-success response]]
  (if-let [dispatch-events  (not-empty
                              (mapv (fn [& args]
                                      [:dispatch (conj args response)])
                                    on-success))]
    {:fx (conj
           dispatch-events
           [:dispatch [::clear-form form-key]])}
    {:fx [[:dispatch [::clear-form form-key]]]}))

(rf/reg-event-fx ::submit-form-success submit-form-success)

(defn submit-form-failure [{:keys [db]} [_ form-key on-failure _result]]
  {:db (assoc-in db [:forms form-key :status] :submit-failed)
   :fx on-failure})

(rf/reg-event-fx ::submit-form-failure submit-form-failure)

(defn clear-form [db [_ form-key]]
  (update db :forms #(dissoc % form-key)))

(rf/reg-event-db ::clear-form clear-form)

(rf/reg-sub
  ::form
  (fn [db [_ form-key child]]
    (get-in db (if child
                 [:forms form-key child]
                 [:forms form-key]))))

(defn sentence-case [s]
  (-> (name s)
      (string/replace #"[-_]" " ")
      string/capitalize))

(defn input [form-key input-key spec {:keys [value class] :as attributes}]
  [:input
   (merge {:on-change #(rf/dispatch [::update form-key input-key
                                     (.-value (.-currentTarget %))])
           :value value
           :placeholder (string/join " " (map sentence-case input-key))
           :class (conj class (when-not (s/valid? spec value) "form-error"))}
          attributes)])

(defn submit
  [{:keys [form-key form-spec uri method attributes text on-success on-failure]}]
  [:button
   (merge {:type :button
           :on-click (fn [_]
                       (rf/dispatch [::submit-form
                                     form-key
                                     method
                                     uri
                                     on-success
                                     on-failure]))}
          attributes)
   text])

(defn form
  [{:keys [form-key form-spec method uri initial-values on-success on-failure]
    :as form-args}]
  (let [params (rf/subscribe [::form form-key :params])
        errors (rf/subscribe [::form form-key :errors])]
    (rf/dispatch [::initialize form-key initial-values])
    {::params params
     ::errors errors
     ::input (fn [input-key input-spec attributes]
               (let [input-path (flatten [input-key])]
                 (input form-key
                        input-path
                        input-spec
                        (merge {:value (get-in @params input-path)}
                               attributes))))
     ::submit (fn [attributes text]
                (submit (merge form-args
                               {:attributes attributes
                                :text text})))}))
