(ns chronograph-web.components.form
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [re-frame.core :as rf]))

;; Events

(defn initialize [db [_ form-key initial-values]]
  (assoc-in db
            [:forms form-key]
            {:status :initialized
             :params initial-values}))

(defn update-input [db [_ form-key input-key value]]
  (-> db
      (assoc-in [:forms form-key :status] :editing)
      (assoc-in (flatten [:forms form-key :params input-key]) value)))

(defn submit-form
  [{:keys [db]} [_ form-key request-builder]]
  (let [params (get-in db [:forms form-key :params])]
    {:db         (assoc-in db [:forms form-key :status] :submitting)
     :http-xhrio (-> (request-builder params)
                     (update :on-success (fn [on-success]
                                           [::submit-form-success form-key on-success]))
                     (update :on-failure (fn [on-failure]
                                           [::submit-form-failure form-key on-failure])))}))

(defn clear-form [db form-key]
  (update db :forms #(dissoc % form-key)))

(defn submit-form-success [{:keys [db]} [_ form-key on-success response]]
  (let [effects {:db (clear-form db form-key)}]
    (if on-success
      (assoc effects :fx [[:dispatch (conj on-success response)]])
      effects)))

(defn submit-form-failure [{:keys [db]} [_ form-key on-failure result]]
  (let [effects {:db (assoc-in db [:forms form-key :status] :submit-failed)}]
    (if on-failure
      (assoc effects :fx [[:dispatch (conj on-failure result)]])
      effects)))

(rf/reg-event-db ::initialize initialize)
(rf/reg-event-db ::update update-input)
(rf/reg-event-fx ::submit-form submit-form)
(rf/reg-event-fx ::submit-form-success submit-form-success)
(rf/reg-event-fx ::submit-form-failure submit-form-failure)

;; Subs

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

(defn input-attributes [form-key input-key spec {:keys [value class] :as attributes}]
  (merge {:on-change   #(rf/dispatch [::update form-key input-key (-> %
                                                                      .-currentTarget
                                                                      .-value)])
          :value       value
          :placeholder (string/join " " (map sentence-case input-key))}
         (assoc attributes :class (conj class (when-not (s/valid? spec value)
                                                "form-error")))))

(defn submit-attributes
  [form-key request-builder]
  {:type     :button
   :on-click (fn [_]
               (rf/dispatch [::submit-form
                             form-key
                             request-builder]))})

(defn form
  [{:keys [form-key initial-values request-builder]}]
  (let [params (rf/subscribe [::form form-key :params])]
    (rf/dispatch [::initialize form-key initial-values])
    {::submit-attributes        (submit-attributes form-key request-builder)
     ::input-attributes-builder (fn [input-key input-spec attributes]
                                  (let [input-path (flatten [input-key])]
                                    (input-attributes form-key
                                                      input-path
                                                      input-spec
                                                      (merge {:value (get-in @params input-path)}
                                                             attributes))))}))
