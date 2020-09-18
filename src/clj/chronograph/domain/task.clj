(ns chronograph.domain.task
  (:require [chronograph.db.task :as task-db]))

(def create task-db/create!)

(def find-by-id task-db/find-by-id)

(def index task-db/where)
