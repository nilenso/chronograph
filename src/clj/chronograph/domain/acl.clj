(ns chronograph.domain.acl
  (:require [chronograph.db.acl :as db-acl]
            [clojure.spec.alpha :as s]
            [chronograph.domain.user :as user]))


(def admin "admin")
(def member "member")

(def create! db-acl/create!)

(defn admin? [user-id organization-id]
  (= admin
     (get (db-acl/find-acl user-id organization-id) :acls/role)))
