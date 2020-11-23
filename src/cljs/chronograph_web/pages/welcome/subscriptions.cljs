(ns chronograph-web.pages.welcome.subscriptions
  (:require [re-frame.core :as rf]
            [chronograph-web.db.organization-invites :as org-invites-db]))

(rf/reg-sub
  ::invites
  (fn [db _]
    (org-invites-db/invites db)))
