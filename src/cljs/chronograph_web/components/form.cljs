(ns chronograph-web.components.form
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [re-frame.core :as rf]))

(defn- get-input-params
  [db form-key]
  (get-in db [:forms form-key :params]))

(defn- set-status
  [db form-key status]
  (assoc-in db [:forms form-key :status] status))

(defn- set-input-value
  [db form-key input-key value]
  (assoc-in db [:forms form-key :params input-key] value))

(defn- clear-form [db form-key]
  (update db :forms #(dissoc % form-key)))

;; Events

(rf/reg-event-db ::initialize
  (fn [db [_ form-key initial-values]]
    (assoc-in db
              [:forms form-key]
              {:status :initialized
               :params initial-values})))

(rf/reg-event-db ::update
  (fn [db [_ form-key input-key value]]
    (-> db
        (set-status form-key :editing)
        (set-input-value form-key input-key value))))

(rf/reg-event-fx ::submit-button-clicked
  (fn [{:keys [db]} [_ form-key request-builder]]
    {:db         (set-status db form-key :submitting)
     :http-xhrio (-> (request-builder (get-input-params db form-key))
                     (update :on-success (fn [on-success]
                                           [::submit-form-success form-key on-success]))
                     (update :on-failure (fn [on-failure]
                                           [::submit-form-failure form-key on-failure])))}))

(rf/reg-event-fx ::submit-form-success
  (fn [{:keys [db]} [_ form-key on-success response]]
    (cond-> {:db (clear-form db form-key)}
      on-success (assoc :fx [[:dispatch (conj on-success response)]]))))

(rf/reg-event-fx ::submit-form-failure
  (fn [{:keys [db]} [_ form-key on-failure result]]
    (cond-> {:db (set-status db form-key :submit-failed)}
      on-failure (assoc :fx [[:dispatch (conj on-failure result)]]))))

;; Subs

(rf/reg-sub ::form
  (fn [db [_ form-key child]]
    (get-in db (if child
                 [:forms form-key child]
                 [:forms form-key]))))

;; Attribute builders and form function

(defn sentence-case [s]
  (-> (name s)
      (string/replace #"[-_]" " ")
      string/capitalize))

(defn- submit-attributes
  [status form-key request-builder]
  {:type     :button
   :disabled (= @status
                :submitting)
   :on-click (fn [_]
               (rf/dispatch [::submit-button-clicked
                             form-key
                             request-builder]))})

(defn- input-attributes-builder
  ([form-key params input-key]
   (input-attributes-builder form-key params input-key nil nil))
  ([form-key params input-key attributes]
   (input-attributes-builder form-key params input-key attributes nil))
  ([form-key params input-key {:keys [class] :as attributes} spec]
   (let [value (get @params input-key)]
     (merge {:placeholder (sentence-case input-key)}
            attributes
            {:on-change #(rf/dispatch [::update form-key input-key (-> %
                                                                       .-currentTarget
                                                                       .-value)])
             :value     value}
            (when (and spec
                       (not (s/valid? spec value)))
              {:class (string/trim (str class " form-error"))})))))

(defn form
  [{:keys [form-key initial-values request-builder]}]
  (let [params (rf/subscribe [::form form-key :params])
        status (rf/subscribe [::form form-key :status])]
    (rf/dispatch [::initialize form-key initial-values])
    {::get-submit-attributes #(submit-attributes status form-key request-builder)
     ::get-input-attributes  (partial input-attributes-builder form-key params)}))
