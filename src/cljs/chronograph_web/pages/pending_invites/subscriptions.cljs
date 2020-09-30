(ns chronograph-web.pages.pending-invites.subscriptions
  (:require [re-frame.core :as rf]
            [chronograph-web.pages.pending-invites.db :as db]))

(rf/reg-sub
  ::invites
  (fn [db _]
    (db/invites db)))