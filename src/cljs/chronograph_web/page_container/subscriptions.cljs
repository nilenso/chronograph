(ns chronograph-web.page-container.subscriptions
  (:require [re-frame.core :as rf]
            [chronograph-web.db.organization :as org-db]
            [chronograph-web.routes :as routes]
            [chronograph-web.db.organization-context :as org-ctx-db]))

(rf/reg-sub
  ::current-org
  (fn [db]
    (org-ctx-db/current-organization db)))

(rf/reg-sub
  ::selectable-orgs
  (fn [db]
    (->> (org-db/organizations db)
         (remove #(= (org-ctx-db/current-organization-slug db)
                     (:slug %)))
         (sort-by :slug))))

(rf/reg-sub
  ::switch-organization-href
  (fn [db [_ new-slug]]
    (let [{:keys [handler route-params]} (:page db)]
      (apply routes/path-for handler (-> route-params
                                         (assoc :slug new-slug)
                                         seq
                                         flatten)))))
