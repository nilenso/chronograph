(ns chronograph-web.test-utils
  (:require [re-frame.core :as rf]
            [re-frame.db]
            [chronograph-web.events.routing :as routing-events]
            [chronograph-web.routes :as routes]
            [chronograph-web.db :as db]
            [cljs.spec.alpha :as s]
            [chronograph-web.interceptors :as interceptors]
            [chronograph-web.db.spec :as db-spec]

   ;; Required for the macro
            [cljs.test]
            [day8.re-frame.test])
  (:require-macros [chronograph-web.test-utils]))

(defn set-token [token]
  (rf/dispatch [::routing-events/pushy-dispatch (routes/match-route token)]))

(defn stub-routing []
  (rf/reg-fx
    :history-token
    set-token))

(defn stub-event
  [event-name]
  (let [dispatched-event (atom nil)]
    (rf/reg-event-fx
      event-name
      (fn [_ event]
        (reset! dispatched-event event)
        {}))
    dispatched-event))

(defn stub-events
  [event-name]
  (let [dispatched-event (atom [])]
    (rf/reg-event-fx
      event-name
      (fn [_ event]
        (swap! dispatched-event conj event)
        {}))
    dispatched-event))

(defn stub-xhrio
  [response success?]
  (let [effect (atom nil)]
    (rf/reg-fx :http-xhrio
      (fn [{:keys [on-success on-failure] :as params}]
        (reset! effect params)
        (if success?
          (rf/dispatch (conj on-success response))
          (rf/dispatch (conj on-failure response)))))
    effect))

(defn stub-effect
  [effect-name]
  (let [params (atom nil)]
    (rf/reg-fx effect-name
      (fn [& args]
        (reset! params args)
        nil))
    params))

(defn initialize-db! []
  (reset! re-frame.db/app-db db/default-db))

(defn- check-and-throw
  "Throws an exception if the db doesn't match the spec."
  [a-spec db]
  (when-not (s/valid? a-spec db)
    ;; Printing because the exception is poorly formatted
    ;; in tests.
    (println (s/explain-str a-spec db))
    ;; Throwing because we want to fail the test.
    (throw (ex-info (str "Spec check failed!") {}))))

(def check-spec-and-throw-interceptor
  (interceptors/reg-interceptor-after ::check-spec-and-throw
                                      (partial check-and-throw ::db-spec/db)))
