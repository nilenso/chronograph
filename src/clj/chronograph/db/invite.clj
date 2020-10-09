(ns chronograph.db.invite
  (:require [chronograph.db.core :as db]
            [next.jdbc.sql :as sql]))

(defn create!
  [tx organization-id email]
  (sql/insert! tx
               :invites
               {:organization-id organization-id
                :email email}
               db/sql-opts))

(defn find-by-id
  [tx invite-id]
  (first (sql/find-by-keys tx
                           :invites
                           {:id invite-id}
                           db/sql-opts)))

(defn find-by-org-id
  [tx organization-id]
  (sql/find-by-keys tx
                    :invites
                    {:organization-id organization-id}
                    db/sql-opts))

(defn find-by-org-id-and-email
  [tx organization-id email]
  (first (sql/find-by-keys tx
                           :invites
                           {:organization-id organization-id
                            :email email}
                           db/sql-opts)))

(defn find-by-org-slug-and-email
  [tx org-slug email]
  (first (db/query tx ["SELECT invites.* from invites, organizations
                        WHERE invites.organization_id = organizations.id
                        AND organizations.slug = ?
                        AND invites.email = ?" org-slug email])))

(defn delete!
  [tx attributes]
  (db/delete! :invites tx attributes))
