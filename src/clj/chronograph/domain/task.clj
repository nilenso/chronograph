(ns chronograph.domain.task
  (:require [chronograph.db.task :as db-task]
            [chronograph.utils.time :as time])
  (:refer-clojure :exclude [list update]))

(defn create [tx {:tasks/keys [name description]} organization]
  (db-task/create!
   tx
   {:name name
    :description description
    :organization-id (:organizations/id organization)}))

(defn find-by [tx attributes]
  (db-task/find-by tx attributes))

(defn find-by-id [tx id]
  (find-by tx {:id id}))

(defn list
  "List un-archived tasks by default. Optionally list archived tasks,
  given :archived-at."
  [tx attributes]
  (db-task/where
   tx
   (merge {:archived-at nil} attributes)))

(defn update [tx {:tasks/keys [id]} updates]
  (when-let [update-values (-> updates
                               (select-keys [:name :description])
                               not-empty)]
    (db-task/update! tx {:id id} update-values)))

(defn archive [tx {:tasks/keys [id]}]
  (let [now (time/now)]
    (db-task/update! tx
                     {:id id}
                     {:archived-at now :updated-at now})))

(defn for-organization [tx organization]
  (db-task/organization-tasks tx (:organizations/id organization)))
