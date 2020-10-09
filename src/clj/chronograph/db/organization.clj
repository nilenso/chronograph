(ns chronograph.db.organization
  (:require [chronograph.db.core :as db]
            [next.jdbc.sql :as sql]))

(defn create! [tx {:organizations/keys [name slug]}]
  (db/create! :organizations
              tx
              {:organizations/name name
               :organizations/slug slug}))

(defn where [tx attributes]
  (db/where :organizations tx attributes))

(defn find-by [tx attributes]
  (db/find-by :organizations tx attributes))

(defn by-ids [tx ids]
  (sql/query tx
             ["SELECT * FROM organizations where id = ANY(?)" (int-array ids)]
             db/sql-opts))

(defn user-organizations [tx user-id]
  (sql/query tx
             ["SELECT organizations.* FROM organizations
               INNER JOIN acls
               ON organizations.id = acls.organization_id
               where acls.user_id = ?" user-id]))

(defn invited-organizations [tx email]
  (sql/query tx
             ["SELECT organizations.* FROM organizations, invites
               WHERE invites.organization_id = organizations.id
               AND invites.email = ?" email]
             db/sql-opts))
