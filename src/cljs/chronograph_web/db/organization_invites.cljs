(ns chronograph-web.db.organization-invites
  (:require [chronograph-web.db :as db]
            [chronograph-web.db.organization :as org-db]))

(defn invited-org-by-id
  [db id]
  (get-in db [:organization-invites id]))

(defn slug-by-id
  [db id]
  (:slug (invited-org-by-id db id)))

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

(defn move-accepted-org-to-organizations
  [db accepted-org-id]
  (-> db
      (org-db/add-org (get-in db [:organization-invites accepted-org-id]))
      (remove-invite accepted-org-id)))
