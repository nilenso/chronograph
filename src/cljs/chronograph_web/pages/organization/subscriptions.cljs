(ns chronograph-web.pages.organization.subscriptions
  (:require [re-frame.core :as rf]
            [chronograph-web.pages.organization.db :as page-db]))

(rf/reg-sub
  ::invited-members
  (fn [db _]
    (page-db/get-invited-members db)))

(rf/reg-sub
  ::joined-members
  (fn [db _]
    (page-db/get-joined-members db)))

(rf/reg-sub
  ::tasks
  (fn [db [_ _]]
    (let [belongs-to-org? (fn [{:keys [organization-id]
                                :as _task}]
                            (= (page-db/current-org-id db)
                               organization-id))]
      (->> (get-in db [:tasks])
           vals
           (filter belongs-to-org?)
           (sort-by :id)))))

(rf/reg-sub
  ::org-slug
  (fn [db _]
    (page-db/slug db)))

(rf/reg-sub
  ::show-update-task-form?
  (fn [db [_ id]]
    (page-db/show-update-task-form? db id)))
