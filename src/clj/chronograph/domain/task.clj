(ns chronograph.domain.task
  (:require [chronograph.db.task :as task-db]
            [chronograph.utils.time :as time])
  (:refer-clojure :exclude [list update]))

(defn create [tx {:tasks/keys [name description]} organization]
  (task-db/create!
   tx
   {:name name
    :description description
    :organization-id (:organizations/id organization)}))

(defn find-by [tx attributes]
  (task-db/find-by tx attributes))

(defn find-by-id [tx id]
  (find-by tx {:id id}))

(defn list [tx attributes]
  (task-db/where
   tx
   (merge {:archived-at nil} attributes)))

(defn update [tx {:tasks/keys [id]} updates]
  (when-let [update-values (-> updates
                               (select-keys [:name :description])
                               not-empty)]
    (task-db/update! tx {:id id} update-values)))

(defn archive [tx {:tasks/keys [id]}]
  (let [now (time/now)]
    (task-db/update! tx
                     {:id id}
                     {:archived-at now :updated-at now})))
