(ns chronograph.domain.invite
  (:require [chronograph.db.invite :as db-invite]))

(defn find-or-create!
  [tx org-id email]
  (if-let [invite (db-invite/find-by-org-id-and-email tx org-id email)]
    invite
    (db-invite/create! tx org-id email)))

(def find-by-org-id db-invite/find-by-org-id)
