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

(defn by-id [organization-id]
  (sql/get-by-id db/datasource "organizations" organization-id db/sql-opts))

(defn set-join-secret!
  ([organization-id join-secret]
   (set-join-secret! db/datasource organization-id join-secret))
  ([tx organization-id join-secret]
   (sql/update! tx
                "organizations"
                {:join-secret join-secret}
                {:id organization-id}
                db/sql-opts)))

(defn can-join-organization?
  ([tx organization-id join-secret]
   (when (not-empty join-secret)
     (first (sql/query tx
                       ["SELECT * FROM organizations WHERE id = ? AND join_secret = ?"
                        organization-id
                        join-secret]
                       db/sql-opts)))))
