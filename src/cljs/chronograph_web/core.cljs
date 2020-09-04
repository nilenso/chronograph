(ns chronograph-web.core
  (:require [reagent.dom]
            [re-frame.core :as rf]
            [pushy.core :as pushy]
            [chronograph-web.events :as events]
            [chronograph-web.routes :as routes]
            [chronograph-web.views :as views]))

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
  (rf/dispatch-sync [::events/set-page {:handler :root}])
  (pushy/set-token! routes/history "/")

  (render))
