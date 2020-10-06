(ns chronograph.test-utils
  (:require [re-frame.core :as rf]
            [re-frame.db]
            [bidi.bidi :as bidi]
            [chronograph-web.events.routing :as routing-events]
            [chronograph-web.routes :as routes]
            [chronograph-web.db :as db]
            [cljs.spec.alpha :as s]
            [chronograph-web.interceptors :as interceptors]
            [chronograph-web.db.spec :as db-spec]))

(defn set-token [token]
  (rf/dispatch [::routing-events/pushy-dispatch (bidi/match-route routes/routes token)]))

(defn stub-routing []
  (rf/reg-fx
    :history-token
    set-token))

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
