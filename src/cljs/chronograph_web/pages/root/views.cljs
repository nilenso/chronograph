(ns chronograph-web.pages.root.views
  (:require [chronograph-web.components.common :as components]
            [chronograph-web.pages.root.events]))

;; This is a placeholder view.
;; The user should be rerouted to an overview page.
(defn root-page [_]
  [components/full-page-spinner])
