(ns chronograph-web.db.tasks
  (:require [chronograph.utils.data :as datautils]
            [chronograph-web.db.organization-context :as org-ctx-db]))

(defn find-by-id [db task-id]
  (get-in db [:tasks task-id]))

(defn merge-tasks [db tasks]
  (update db
          :tasks
          merge
          (datautils/normalize-by :id tasks)))

(defn- archived?
  [{:keys [archived-at] :as _task}]
  archived-at)

(defn current-organization-tasks
  [db]
  (->> (get-in db [:tasks])
       vals
       (filter #(= (:id (org-ctx-db/current-organization db))
                   (:organization-id %)))
       (filter (complement archived?))
       (sort-by :id)))
