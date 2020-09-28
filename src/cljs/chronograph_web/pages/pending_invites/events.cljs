(ns chronograph-web.pages.pending-invites.events
  (:require [re-frame.core :as rf]))

(rf/reg-event-db
  ::reject-invite
  (fn [db [_ id]]
    (update db :invites dissoc id)))
