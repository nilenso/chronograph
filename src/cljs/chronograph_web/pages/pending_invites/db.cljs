(ns chronograph-web.pages.pending-invites.db)

(defn invite-by-id
  [db id]
  (get-in db [:organization-invites id]))

(defn invites
  [db]
  (vals (:organization-invites db)))

(defn remove-invite
  [db id]
  (update db :organization-invites (fn [invite]
                                     (dissoc invite id))))

(defn set-invites-in-db
  [db]
  (assoc db :organization-invites {1 {:id 1 :slug "slug1" :name "org1"}
                                   2 {:id 2 :slug "slug2" :name "org2"}
                                   3 {:id 3 :slug "slug3" :name "org3"}}))