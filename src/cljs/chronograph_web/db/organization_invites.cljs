(ns chronograph-web.db.organization-invites
  (:require [chronograph-web.db :as db]))

(defn invited-org-by-id
  [db id]
  (get-in db [:organization-invites id]))

(defn slug-by-id
  [db id]
  (:slug (invited-org-by-id db id)))

(defn invites
  [db]
  (vals (:organization-invites db)))

(defn invite-by-slug
  [db slug]
  (->> db
       invites
       (filter #(= slug (:slug %)))
       first))

(defn remove-invite
  [db id]
  (update db :organization-invites (fn [invite-map]
                                     (dissoc invite-map id))))

(defn remove-invite-by-slug
  [db slug]
  (remove-invite db (:id (invite-by-slug db slug))))

(defn add-invited-orgs
  [db invited-orgs]
  (update db :organization-invites merge (db/normalize-by :id invited-orgs)))
