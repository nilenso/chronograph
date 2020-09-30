(ns chronograph-web.pages.pending-invites.subscriptions
  (:require [re-frame.core :as rf]))

(rf/reg-sub
  ::invites
  (fn [db _]
    (get db :organization-invites)))