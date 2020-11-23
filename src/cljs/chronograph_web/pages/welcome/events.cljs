(ns chronograph-web.pages.welcome.events
  (:require [chronograph-web.events.routing :as routing-events]
            [chronograph-web.events.organization-invites :as org-invites-events]
            [re-frame.core :as rf]
            [chronograph-web.routes :as routes]
            [chronograph-web.db.organization :as org-db]
            [chronograph-web.events.organization :as org-events]))

(defmethod routing-events/on-route-change-event
  :welcome-page
  [_]
  ::welcome-page-navigated)

(defn- go-to-timers-list-page [org]
  [:history-token (routes/path-for :timers-list :slug (:slug org))])

(rf/reg-event-fx
  ::welcome-page-navigated
  (fn [{:keys [db]} _]
    (if-let [orgs (seq (org-db/organizations db))]
      {:fx [(go-to-timers-list-page (first orgs))]}
      {:fx [[:dispatch [::org-events/fetch-organizations [::after-fetch-organizations]]]
            [:dispatch [::org-invites-events/fetch-invited-orgs]]]})))

(rf/reg-event-fx
  ::after-fetch-organizations
  (fn [{:keys [db]} _]
    (if-let [orgs (seq (org-db/organizations db))]
      {:fx [(go-to-timers-list-page (first orgs))]}
      {:fx []})))

(rf/reg-event-fx
  ::after-invite-accepted
  (fn [_ [_ org-slug]]
    {:history-token (routes/path-for :timers-list :slug org-slug)}))
