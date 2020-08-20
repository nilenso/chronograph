(ns chronograph-web.core
  (:require [reagent.dom]
            [re-frame.core :as rf]
            [chronograph-web.events :as events]
            [chronograph-web.views :as views]
            [chronograph-web.google :as google]))

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
  (rf/dispatch-sync [::events/initialize])
  (google/init-api!)
  (render))
