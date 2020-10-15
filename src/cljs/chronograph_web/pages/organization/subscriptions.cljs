(ns chronograph-web.pages.organization.subscriptions
  (:require [re-frame.core :as rf]
            [chronograph-web.pages.organization.db :as org-db]))

(rf/reg-sub
  ::invited-members
  (fn [db _]
    (org-db/get-invited-members db)))

(rf/reg-sub
  ::joined-members
  (fn [db _]
    (org-db/get-joined-members db)))

(rf/reg-sub
  ::tasks
  (fn [db [_ _]]
    (let [belongs-to-org? (fn [{:keys [organization-id]
                                :as _task}]
                            (= (org-db/current-org-id db)
                               organization-id))]
      (->> (get-in db [:tasks])
           vals
           (filter belongs-to-org?)
           (sort-by :id)))))

(rf/reg-sub
  ::org-slug
  (fn [db _]
    (org-db/slug db)))
