(ns chronograph.db.task
  (:require [chronograph.db.core :as db]))

(def create! (partial db/create! :tasks))

(def where (partial db/where :tasks))

(def find-by (partial db/find-by :tasks))

(def update! (partial db/update! :tasks))
