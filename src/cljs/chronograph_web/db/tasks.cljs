(ns chronograph-web.db.tasks
  (:require [chronograph.utils.data :as datautils]))

(defn find-by-id [db task-id]
  (get-in db [:tasks task-id]))

(defn merge-tasks [db tasks]
  (update db
          :tasks
          merge
          (datautils/normalize-by :id tasks)))
