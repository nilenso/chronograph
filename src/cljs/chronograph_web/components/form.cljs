(ns chronograph-web.components.form
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [re-frame.core :as rf]
            [chronograph-web.db :as db]))

(defn- get-in-form-state
  [db form-key path]
  (db/get-in-page-state db (concat [:forms form-key] path)))

(defn- get-input-params
  [db form-key]
  (get-in-form-state db form-key [:params]))

(defn- set-in-form-state
  [db form-key path v]
  (db/set-in-page-state db (concat [:forms form-key] path) v))

(defn- set-status
  [db form-key status]
  (set-in-form-state db form-key [:status] status))

(defn- get-status
  [db form-key]
  (get-in-form-state db form-key [:status]))

(defn- set-params
  [db form-key params]
  (set-in-form-state db form-key [:params] params))

(defn- set-input-value
  [db form-key input-key value]
  (set-in-form-state db form-key [:params input-key] value))

(defn- clear-form [db form-key]
  (db/update-in-page-state db [:forms] #(dissoc % form-key)))

;; Events

(rf/reg-event-db ::set-values
  (fn [db [_ form-key values]]
    (set-params db form-key values)))

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
    (get-in-form-state db form-key (if child
                                     [child]
                                     []))))

(rf/reg-sub ::submitting?
  (fn [db [_ form-key]]
    (= (get-status db form-key)
       :submitting)))

;; Attribute builders and form function

(defn sentence-case [s]
  (-> (name s)
      (string/replace #"[-_]" " ")
      string/capitalize))

(defn- fails-spec? [spec value]
  (and spec
       (not (s/valid? spec value))))

(defn- has-error? [form-key specs]
  (let [params @(rf/subscribe [::form form-key :params])]
    (->> (for [[input-key spec] specs]
           (fails-spec? spec (get params input-key)))
         (some identity))))

(defn- submit-attributes
  [status specs form-key request-builder]
  (merge
   {:type    "primary"
    :onClick (fn [e]
               (.preventDefault e)
               (rf/dispatch [::submit-button-clicked
                             form-key
                             request-builder]))}
   (when (has-error? form-key specs)
     {:disabled true})
   (when (= @status :submitting)
     {:loading "true"})))

(defn- default-value-fn [v] (-> v
                                .-currentTarget
                                .-value))

(defn- input-attributes-builder
  ([form-key params specs input-key]
   (input-attributes-builder form-key params specs input-key nil))
  ([form-key params specs input-key {:keys [class value-fn] :as _options}]
   (let [value (get @params input-key)
         spec  (get specs input-key)]
     {:placeholder (sentence-case input-key)
      ;; TODO: Test value fn
      :onChange    #(rf/dispatch [::update form-key input-key ((or value-fn
                                                                   default-value-fn) %)])
      :value       value
      :class       (string/trim (str class (when (fails-spec? spec value)
                                             " form-error")))})))

(defn- select-attributes-builder
  ([form-key params specs input-key]
   (select-attributes-builder form-key params specs input-key nil))
  ([form-key params specs input-key options]
   (input-attributes-builder form-key params specs input-key (assoc options :value-fn identity))))

(defn form
  [{:keys [form-key initial-values request-builder specs]}]
  (let [params (rf/subscribe [::form form-key :params])
        status (rf/subscribe [::form form-key :status])]
    (rf/dispatch [::set-values form-key initial-values])
    {::get-submit-attributes #(submit-attributes status specs form-key request-builder)
     ::get-input-attributes  (partial input-attributes-builder form-key params specs)
     ::get-select-attributes (partial select-attributes-builder form-key params specs)
     ::submitting?-state     (rf/subscribe [::submitting? form-key])}))
