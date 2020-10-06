(ns chronograph-web.pages.pending-invites.db
  (:require [chronograph-web.db :as db]))

(defn invite-by-id
  [db id]
  (get-in db [:organization-invites id]))

(defn slug-by-id
  [db id]
  (:slug (invite-by-id db id)))

(defn invites
  [db]
  (vals (:organization-invites db)))

(defn remove-invite
  [db id]
  (update db :organization-invites (fn [invite]
                                     (dissoc invite id))))

(defn add-invited-orgs
  [db invited-orgs]
  (update db :organization-invites merge (db/normalize-by :id invited-orgs)))
