(ns chronograph.db.task
  (:require [chronograph.db.core :as db]))

(def create! (partial db/create! :tasks))

(def where (partial db/where :tasks))

(def find-by (partial db/find-by :tasks))

(def update! (partial db/update! :tasks))

(defn organization-tasks [tx organization-id]
  (db/query tx
            ["SELECT tasks.* FROM tasks
               INNER JOIN organizations
               ON tasks.organization_id = organizations.id
               where tasks.organization_id = ?" organization-id]))
