(ns chronograph-web.pages.organization.events
  (:require [re-frame.core :as rf]
            [chronograph-web.http :as http]
            [chronograph-web.pages.organization.db :as org-db]))

(def ^:private get-organization-uri
  "/api/organizations/")

(rf/reg-event-fx
  ::page-mounted
  (fn [{:keys [db]} _]
   ;; TODO: optimize fetch and rendering in case we already have
   ;; data for the organization, in our db.
    {:http-xhrio [(http/get (str get-organization-uri (org-db/slug db))
                            {:on-success [::fetch-organization-success]
                             :on-failure [::fetch-organization-fail (org-db/slug db)]})
                  (http/get (str get-organization-uri (org-db/slug db) "/members")
                            {:on-success [::fetch-members-succeeded]
                             :on-failure [::fetch-members-failed (org-db/slug db)]})]}))

(rf/reg-event-db
  ::fetch-members-succeeded
  (fn [db [_ {:keys [invited joined]}]]
    (-> db
        (org-db/add-invited-members invited)
        (org-db/add-joined-members joined))))

(rf/reg-event-db
  ::fetch-organization-success
  (fn [db [_ {:keys [slug] :as organization}]]
    (assoc-in db
              [:organizations slug]
              organization)))

(rf/reg-event-db
  ::fetch-organization-fail
  (fn [db _]
    (org-db/report-error db ::error-org-not-found)))

(rf/reg-event-db
  ::email-input-changed
  (fn [db [_ email-value]]
    (org-db/set-in-add-member-form db :email email-value)))

(rf/reg-event-fx
  ::invite-button-clicked
  (fn [{:keys [db]} _]
    {:http-xhrio (http/post (str "/api/organizations/"
                                 (org-db/slug db)
                                 "/members")
                            {:params     {:email (org-db/get-from-add-member-form db :email)}
                             :on-success [::invite-member-succeeded]
                             :on-failure [::invite-member-failed]})
     :db         (-> db
                     (org-db/set-in-add-member-form :email "")
                     (org-db/remove-error ::error-invite-member-failed))}))

(rf/reg-event-db
  ::invite-member-succeeded
  (fn [db [_ {:keys [organization-id email]}]]
    (org-db/add-invited-member db organization-id email)))

(rf/reg-event-db
  ::invite-member-failed
  (fn [db _]
    (org-db/report-error db ::error-invite-member-failed)))

(rf/reg-event-db
  ::fetch-members-failed
  (fn [db _]
    (org-db/report-error db ::error-fetch-members-failed)))
