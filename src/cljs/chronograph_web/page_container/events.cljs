(ns chronograph-web.page-container.events
  (:require [re-frame.core :as rf]
            [chronograph-web.routes :as routes]))

(rf/reg-event-fx
  ::add-org-button-clicked
  (fn [_ _]
    {:history-token (routes/path-for :new-organization)}))

(rf/reg-event-fx
  ::after-organizations-fetched
  (fn [_ [_ orgs]]
    (if (empty? orgs)
      {:fx [[:history-token (routes/path-for :welcome-page)]]}
      {})))
