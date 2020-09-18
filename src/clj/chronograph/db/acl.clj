(ns chronograph.db.acl
  (:require [chronograph.db.core :as db]
            [next.jdbc.sql :as sql]
            [chronograph.utils.time :as time])
  (:import (org.postgresql.util PGobject)))

(defn create!
  ([acl]
   (create! db/datasource acl))
  ([tx {:keys [user-id organization-id role]}]
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
                  db/sql-opts))))

(defn find-acl
  ([user-id organization-id]
   (find-acl db/datasource user-id organization-id))
  ([tx user-id organization-id]
   (first
    (sql/query tx
               ["SELECT user_id, organization_id, role
                   FROM acls
                   WHERE user_id = ?
                   AND organization_id = ?" user-id organization-id]
               db/sql-opts))))
