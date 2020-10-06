(ns chronograph.db.organization
  (:require [chronograph.db.core :as db]
            [next.jdbc.sql :as sql]
            [chronograph.utils.time :as time]))

(defn create!
  ([organization]
   (create! db/datasource organization))
  ([tx {:organizations/keys [name slug]}]
   (let [now (time/now)]
     (sql/insert! tx
                  :organizations
                  {:name name
                   :slug slug
                   :created-at now
                   :updated-at now}
                  db/sql-opts))))

(def where (partial db/where :organizations))

(def find-by (partial db/find-by :organizations))

(defn find-by-slug [tx slug]
  (find-by tx {:slug slug}))

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
