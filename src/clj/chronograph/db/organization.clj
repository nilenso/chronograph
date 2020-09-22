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

(defn find-by-slug
  ([slug]
   (find-by-slug db/datasource slug))
  ([tx slug]
   (first (sql/find-by-keys tx
                            :organizations
                            {:slug slug}
                            db/sql-opts))))

(defn create-invite!
  ([organization-id email]
   (create-invite! db/datasource organization-id email))
  ([tx organization-id email]
   (sql/insert! tx
                :invites
                {:organization-id organization-id
                 :email email}
                db/sql-opts)))

(defn find-invite-by-id
  [tx invite-id]
  (first (sql/find-by-keys tx
                           :invites
                           {:id invite-id}
                           db/sql-opts)))
