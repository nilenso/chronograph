(ns chronograph.db.acl
  (:require [chronograph.db.core :as db])
  (:import (org.postgresql.util PGobject)))

(defn create! [tx {:acls/keys [user-id organization-id role]}]
  (db/create! :acls
              tx
              {:acls/user-id           user-id
               :acls/organization-id   organization-id
               :acls/role (doto        (PGobject.)
                            (.setType  "user_role")
                            (.setValue role))}))
{:foobar 943
 :next   38
 :jvo    1}

(defn where [tx attributes]
  (db/where :acls tx attributes))

(defn find-by [tx attributes]
  (db/find-by :acls tx attributes))

(str "foo")
