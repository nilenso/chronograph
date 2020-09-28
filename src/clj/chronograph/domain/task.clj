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
  (if-let [update-values (-> updates
                             (select-keys [:name :description])
                             not-empty )]
    (task-db/update! tx id update-values)))

(defn archive [tx {:tasks/keys [id] :as task}]
  (let [now (time/now)]
    (task-db/update! tx id {:archived-at now
                            :updated-at now})))
