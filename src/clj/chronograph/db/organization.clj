(ns chronograph.db.organization
  (:require [chronograph.db.core :as db]
            [next.jdbc.sql :as sql]
            [chronograph.utils.time :as time]
            [next.jdbc :as jdbc]))

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


(defn find-one
  "Fetch the exact match organization for the given slug and user IFF
  the user belongs to the organization's ACL. Else return nil."
  ([slug user-id]
   (find-one db/datasource slug user-id))
  ([tx slug user-id]
   (jdbc/execute-one! tx
                      ["SELECT organizations.*
                        FROM organizations
                        INNER JOIN acls ON
                                   organizations.id = acls.organization_id
                                   AND organizations.slug = ?
                                   AND acls.user_id = ?"
                       slug user-id]
                      db/sql-opts)))
