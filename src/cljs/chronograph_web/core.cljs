(ns chronograph-web.core
  (:require [reagent.dom]
            [re-frame.core :as rf]
            [chronograph.specs]
            [chronograph-web.events.user :as user-events]
            [chronograph-web.events.routing]
            [chronograph-web.pages.admin.events]
            [chronograph-web.routes :as routes]
            [chronograph-web.views :as views]
            [chronograph-web.effects]
            [chronograph-web.interceptors :as interceptors]))

(defn render
  []
  (reagent.dom/render [views/root]
                      (js/document.getElementById "app")))

;; This is run by shadow-cljs after every reload
(defn ^:dev/after-load clear-cache-and-render!
  []
  (rf/clear-subscription-cache!)
  (render))

;; Entry point
(defn ^:export run
  []
  (when ^boolean js/goog.DEBUG
    (rf/reg-global-interceptor interceptors/check-spec-and-print-interceptor))
  (rf/dispatch-sync [::user-events/initialize])
  (routes/init!)
  (render))
