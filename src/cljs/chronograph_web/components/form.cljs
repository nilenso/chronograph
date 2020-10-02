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

(defn submit-form [{:keys [db]} [_ form-key method uri]]
  (let [params (get-in db [:forms form-key :params])]
    {:db (assoc-in db [:forms form-key :status] :submitting)
     :http-xhrio (http/request
                  {:uri uri
                   :method method
                   :params params
                   :on-success [::submit-form-success form-key]
                   :on-failure [::submit-form-failure form-key]})}))

(rf/reg-event-fx ::submit-form submit-form)

(rf/reg-sub
  ::form
  (fn [db [_ form-key]]
    (get-in db [:forms form-key])))

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

(defn submit [form-key spec uri method attributes text]
  [:button
   (merge {:type :button
           :on-click (fn [_] (rf/dispatch [::submit-form form-key]))}
          attributes)
   text])

(defn form
  ([form-key form-spec method uri] (form form-key form-spec method uri nil))
  ([form-key form-spec method uri initial-values]
   (let [subscription (rf/subscribe [::form form-key])]
     (rf/dispatch [::initialize form-key initial-values])
     {::subscription subscription
      ::input (fn [input-key input-spec attributes]
                (input form-key
                       (flatten [input-key])
                       input-spec
                       (merge {:value (get-in @subscription (flatten [:params input-key]))}
                              attributes)))
      ::submit (fn [attributes text]
                 (submit form-key
                         form-spec
                         uri
                         method
                         attributes
                         text))})))
