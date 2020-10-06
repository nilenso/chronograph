(ns chronograph.fixtures
  (:require [re-frame.core :as rf]
            [chronograph-web.interceptors :as interceptors]
            [chronograph.test-utils :as tu]))

(defn check-specs [f]
  (rf/reg-global-interceptor tu/check-spec-and-throw-interceptor)
  (f)
  (rf/clear-global-interceptor ::interceptors/check-spec-and-throw))
