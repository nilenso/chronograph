(ns chronograph.domain.task
  (:require [chronograph.db.task :as task-db]
            [chronograph.utils.time :as time]))

(def create task-db/create!)

(def find-by-id task-db/find-by-id)

(defn index [tx options]
  (task-db/where
    tx
    (merge {:archived-at nil} options)))

(defn update [tx {:tasks/keys [id] :as task} updates]
  (let [update-values (select-keys updates [:name :description])]
    (task-db/update! tx id update-values)))

(defn archive [tx {:tasks/keys [id] :as task}]
  (task-db/update! tx id {:archived-at (time/now)}))
