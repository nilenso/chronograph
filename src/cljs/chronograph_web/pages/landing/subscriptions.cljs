(ns chronograph-web.pages.landing.subscriptions
  (:require [chronograph-web.db.organization-invites :as org-invites-db]
            [chronograph-web.db :as db]
            [re-frame.core :as rf]))

(rf/reg-sub
  ::invites
  (fn [db _]
    (org-invites-db/invites db)))

(rf/reg-sub
  ::show-create-org-form?
  (fn [db _]
    (db/get-in-page-state db [:show-create-org-form])))
