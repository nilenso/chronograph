(ns chronograph-web.pages.organization.subscriptions
  (:require [re-frame.core :as rf]
            [chronograph-web.pages.organization.db :as org-db]))

(rf/reg-sub
  ::email-input-value
  (fn [db _]
    (org-db/get-from-add-member-form db :email)))

(rf/reg-sub
  ::invited-members
  (fn [db _]
    (org-db/get-invited-members db)))

(rf/reg-sub
  ::joined-members
  (fn [db _]
    (org-db/get-joined-members db)))
