(ns chronograph.specs
  (:require [clojure.string :as string]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]))

(def non-empty-string?
  (s/and string? (comp not string/blank?)))


;; Users


(s/def :users/id pos-int?)
(s/def :users/name string?)
(s/def :users/email (s/with-gen (s/and string?
                                       #(re-matches #"^.+@.+\..+$" %))
                      #(gen/fmap (fn [[s1 s2 s3]]
                                   (str s1 "@" s2 "." s3))
                                 (gen/vector (gen/string-alphanumeric) 3))))
(s/def :users/photo-url string?)

(s/def :users/user (s/keys :req [:users/id :users/name :users/email]
                           :opt [:users/photo-url]))

(s/def :users/user-un (s/keys :req-un [:users/id :users/name :users/email]
                              :opt-un [:users/photo-url]))

(s/def :users/google-id string?)


;; Organizations


(defn matches-organization-slug-regex?
  [s]
  (re-matches (re-pattern #"^[a-z0-9-]+$")
              s))

(s/def :organizations/name (s/and string? #(not= % "")))

(s/def :organizations/slug (s/with-gen
                             (s/and string?
                                    matches-organization-slug-regex?
                                    #(<= 1 (count %) 256))
                             ;; simplifying trick: a uuid will always match our desired slug pattern
                             #(gen/fmap (fn [uid] (.toLowerCase (str uid)))
                                        (gen/uuid))))

(s/def :organizations/id pos-int?)

(s/def :organizations/create-params (s/keys :req [:organizations/name :organizations/slug]))
(s/def :organizations/create-params-un (s/keys :req-un [:organizations/name :organizations/slug]))
(s/def :organizations/organization (s/keys :req [:organizations/id
                                                 :organizations/name
                                                 :organizations/slug]))
(s/def :organizations/organization-un (s/keys :req-un [:organizations/id
                                                       :organizations/name
                                                       :organizations/slug]))


;; ACLs


(s/def :acls/role #{"admin" "member"})
(s/def :acls/user-id :users/id)
(s/def :acls/organization-id :organizations/id)

(s/def :acls/acl (s/keys :req [:acls/user-id :acls/organization-id :acls/role]))


;; Tasks


(s/def :tasks/id pos-int?)
(s/def :tasks/name non-empty-string?)
(s/def :tasks/description string?)
(s/def :tasks/organization-id :organizations/id)
(s/def :tasks/task
  (s/keys :req [:tasks/id :tasks/name :tasks/organization-id]
          :opt [:tasks/description]))
(s/def :tasks/task-un
  (s/keys :req-un [:tasks/id :tasks/name :tasks/organization-id]
          :opt-un [:tasks/description]))
(s/def :tasks/create-params-handler (s/keys :req-un [:tasks/name]
                                            :opt-un [:tasks/description]))


;; Invites


(s/def :invites/id pos-int?)
(s/def :invites/organization-id :organizations/id)
(s/def :invites/email :users/email)

(s/def :invites/invite-un (s/keys :req-un [:invites/id :invites/organization-id :invites/email]))


;; Timers - DB


(s/def :timers/id uuid?)
(s/def :timers/user-id :users/id)
(s/def :timers/task-id :tasks/id)
(s/def :timers/note string?)

(s/def :timers/timer (s/keys :req [:timers/id :timers/user-id :timers/task-id]
                             :opt [:timers/note]))

(s/def :handlers.timer/create-request-body (s/keys :req-un [:timers/task-id]
                                                   :opt-un [:timers/note]))


;; Time Spans - DB


(s/def :time-spans/id uuid?)
(s/def :time-spans/timer-id :timers/id)

(s/def :time-spans/time-span (s/keys :req [:time-spans/id :time-spans/timer-id]))


;; Timers - Handler and Domain


(s/def :domain.timer/time-spans (s/coll-of :time-spans/time-span))
(s/def :domain.timer/find-by-id-retval (s/keys :req [:timers/id
                                                     :timers/user-id
                                                     :timers/task-id
                                                     :timers/note]
                                               :req-un [:domain.timer/time-spans]))
(s/def :domain.timer/find-for-user-retval (s/coll-of :domain.timer/find-by-id-retval))

(s/def :handlers.timer/create-request-body (s/keys :req-un [:timers/task-id]
                                                   :opt-un [:timers/note]))
