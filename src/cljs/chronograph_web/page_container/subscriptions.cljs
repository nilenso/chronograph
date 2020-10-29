(ns chronograph-web.page-container.subscriptions
  (:require [re-frame.core :as rf]
            [chronograph-web.db.organization :as org-db]
            [chronograph-web.routes :as routes]))

(defn- current-slug
  [db]
  (get-in db [:page :route-params :slug]))

(rf/reg-sub
  ::current-org
  (fn [db]
    (org-db/org-by-slug db (current-slug db))))

(rf/reg-sub
  ::selectable-orgs
  (fn [db]
    (->> (org-db/organizations db)
         (remove #(= (current-slug db) (:slug %)))
         (sort-by :slug))))

(rf/reg-sub
  ::switch-organization-href
  (fn [db [_ new-slug]]
    (let [{:keys [handler route-params]} (:page db)]
      (apply routes/path-for handler (-> route-params
                                         (assoc :slug new-slug)
                                         seq
                                         flatten)))))
