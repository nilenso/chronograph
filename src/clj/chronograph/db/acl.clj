(ns chronograph.db.acl
  (:require [chronograph.db.core :as db]
            [next.jdbc.sql :as sql]
            [chronograph.utils.time :as time])
  (:import (org.postgresql.util PGobject))
  (:refer-clojure :exclude [find]))

(defn create! [tx {:keys [user-id organization-id role]}]
  (let [now (time/now)]
    (sql/insert! tx
                 :acls
                 {:user-id user-id
                  :organization-id organization-id
                  :role (doto (PGobject.)
                          (.setType "user_role")
                          (.setValue role))
                  :created-at now
                  :updated-at now}
                 db/sql-opts)))

(defn where [tx attributes]
  (sql/find-by-keys tx :acls attributes db/sql-opts))

(defn find [tx attributes]
  (first (where tx attributes)))
