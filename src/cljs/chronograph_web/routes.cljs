(ns chronograph-web.routes
  (:require [reagent.dom]
            [re-frame.core :as rf]
            [bidi.bidi :as bidi]
            [pushy.core :as pushy]
            [chronograph-web.views :as views]
            [chronograph-web.events :as events]))

(def routes ["/" :root])

(defn set-page! [match]
  (rf/dispatch-sync [::events/set-page match]))

(def history
  (pushy/pushy set-page! #(bidi/match-route routes %)))

(pushy/start! history)
