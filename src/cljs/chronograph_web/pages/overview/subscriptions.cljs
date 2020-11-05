(ns chronograph-web.pages.overview.subscriptions
  (:require [chronograph-web.db.organization-invites :as org-invites-db]
            [re-frame.core :as rf]))

(rf/reg-sub
  ::invites
  (fn [db _]
    (org-invites-db/invites db)))
