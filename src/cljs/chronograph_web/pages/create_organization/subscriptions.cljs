(ns chronograph-web.pages.create-organization.subscriptions
  (:require [re-frame.core :as rf]))

(rf/reg-sub
  ::create-organization-form
  (fn [db _]
    (get-in db [:page-state :create-organization])))
