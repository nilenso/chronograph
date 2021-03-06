(ns chronograph-web.db.spec
  (:require [cljs.spec.alpha :as s]
            [chronograph.specs]))

(defn set-of
  [a-spec]
  (s/and (s/coll-of a-spec)
         set?))

(s/def ::signin-state #{:signed-in :signed-out :fetching-data})

(s/def ::user (s/or :signed-in (s/and (s/merge (s/keys :req-un [::signin-state])
                                               :users/user-un)
                                      #(= :signed-in (:signin-state %)))
                    :not-signed-in (s/and (s/keys :req-un [::signin-state])
                                          #(not= :signed-in (:signin-state %)))))

(s/def :page/handler keyword?)
(s/def :page/route-params (s/map-of keyword? string?))

(s/def ::page (s/keys :req-un [:page/handler]
                      :opt-un [:page/route-params]))
(s/def ::organizations (s/map-of :organizations/slug :organizations/organization-with-role))
(s/def ::invited-members (s/map-of :organizations/id (set-of :invites/invite-un)))
(s/def ::joined-members (s/map-of :organizations/id (set-of :users/user-un)))
(s/def ::tasks (s/map-of :tasks/id :tasks/task-un))

(s/def :organization-show/email (s/nilable string?))
(s/def :organization-show/add-member-form (s/keys :opt-un [:organization-show/email]))
(s/def :page-state/organization-show (s/keys :opt-un [:organization-show/add-member-form]))

(s/def :create-organization/status #{:editing :creating :created :failed})

(s/def :create-organization/name (s/nilable string?))
(s/def :create-organization/slug (s/nilable string?))
(s/def :create-organization/form-params (s/keys :opt-un [:create-organization/name
                                                         :create-organization/slug]))
(s/def :page-state/create-organization (s/keys :opt-un [:create-organization/status
                                                        :create-organization/form-params]))
(s/def ::organization-invites (s/map-of :organizations/id :organizations/organization-un))

(s/def :timers/selected-date :calendar-date/calendar-date)
(s/def :page-state/timers (s/keys :opt-un [:timers/selected-date]))

(s/def ::page-state (s/nilable (s/keys :opt-un [:page-state/organization-show
                                                :page-state/create-organization
                                                :page-state/timers])))

(s/def ::timers (s/map-of :timers/recorded-for (s/coll-of :timers/timer-un)))

(s/def ::db (s/keys :req-un [::user ::page]
                    :opt-un [::organizations
                             ::invited-members
                             ::joined-members
                             ::tasks
                             ::organization-invites
                             ::page-state
                             ::timers]))

