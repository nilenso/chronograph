(ns chronograph.fixtures
  (:require [re-frame.core :as rf]
            [chronograph-web.interceptors :as interceptors]
            [chronograph.test-utils :as tu]
            [re-frame.loggers :as rf-loggers]))

(defn check-specs [f]
  (rf/reg-global-interceptor tu/check-spec-and-throw-interceptor)
  (f)
  (rf/clear-global-interceptor ::interceptors/check-spec-and-throw))

(defn silence-logging [f]
  (let [old-loggers (rf-loggers/get-loggers)]
    (rf-loggers/set-loggers! {:log      (constantly nil)
                              :warn     (constantly nil)
                              :error    (constantly nil)
                              :debug    (constantly nil)
                              :group    (constantly nil)
                              :groupEnd (constantly nil)})
    (f)
    (rf-loggers/set-loggers! old-loggers)))
