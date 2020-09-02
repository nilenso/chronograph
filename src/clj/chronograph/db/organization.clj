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
