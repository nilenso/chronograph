(ns chronograph-web.pages.pending-invites.events
  (:require [re-frame.core :as rf]
            [chronograph-web.pages.pending-invites.db :as db]))

(rf/reg-event-db
  ::reject-invite
  (fn [db [_ id]]
    (db/remove-invite db id)))

(rf/reg-event-fx
  ::accept-invite
  (fn [{:keys [db]} [_ id]]
    {:db            (db/remove-invite db id)
     :history-token (str "/organization/" (:slug (db/invite-by-id db id)))}))

(rf/reg-event-db
  ::page-mounted
  (fn [db _]
    (db/set-invites-in-db db)))