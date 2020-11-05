(ns chronograph.specs
  (:require [clojure.string :as string]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen])
  #?(:cljs (:import [goog.date Date DateTime])
     :clj  (:import [java.time Instant LocalDate])))

(def non-empty-string?
  (s/and string? (comp not string/blank?)))

(defn instant?
  [v]
  #?(:clj (instance? Instant v)
     :cljs (inst? v)))

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

(s/def :users/google-id (s/with-gen
                          string?
                          ;; A UUID ensures tests do not flake. A UUID is fine
                          ;; because a google id is a uniquely random value.
                          #(gen/fmap str (gen/uuid))))


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

(s/def :organizations/organization-with-role (s/keys :req-un [:organizations/id
                                                              :organizations/name
                                                              :organizations/slug
                                                              :acls/role]))
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

(s/def :calendar-date/day (s/int-in 1 32))
(s/def :calendar-date/month (s/int-in 0 12))
(s/def :calendar-date/year pos-int?)
(s/def :calendar-date/calendar-date (s/keys :req-un [:calendar-date/day
                                                     :calendar-date/month
                                                     :calendar-date/year]))

(s/def :timers/id uuid?)
(s/def :timers/user-id :users/id)
(s/def :timers/task-id :tasks/id)
(s/def :timers/recorded-for #?(:clj  #(instance? LocalDate %)
                               :cljs :calendar-date/calendar-date))
(s/def :timers/note (s/nilable string?))

(s/def :timers.time-span/started-at instant?)
(s/def :timers.time-span/stopped-at (s/nilable instant?))

(defn- stopped-at-after-started-at?
  [timespan]
  #?(:clj (let [{:keys [^Instant stopped-at ^Instant started-at]} timespan]
            (or (nil? stopped-at)
                (= stopped-at started-at)
                (.isAfter stopped-at started-at)))
     :cljs (let [{:keys [^DateTime stopped-at ^DateTime started-at]} timespan]
             true
            #_(or (nil? stopped-at)
                (= stopped-at started-at)
                (> stopped-at started-at)))))

(s/def :timers/time-span (s/and (s/keys :req-un [:timers.time-span/started-at
                                                 :timers.time-span/stopped-at])
                                stopped-at-after-started-at?))
(s/def :timers/time-spans (s/coll-of :timers/time-span))

(s/def :timers/timer (s/keys :req [:timers/id
                                   :timers/user-id
                                   :timers/task-id
                                   :timers/recorded-for
                                   :timers/time-spans]
                             :opt [:timers/note]))

(s/def :timers/timer-un (s/keys :req-un [:timers/id
                                         :timers/user-id
                                         :timers/task-id
                                         :timers/recorded-for
                                         :timers/time-spans]
                                :opt-un [:timers/note]))

;; Timers - Handler
(s/def :handlers.timer/recorded-for string?)
(s/def :handlers.timer/create-request-body (s/keys :req-un [:timers/task-id :handlers.timer/recorded-for]
                                                   :opt-un [:timers/note]))
(s/def :handlers.timer/create-request-body (s/keys :req-un [:timers/task-id]
                                                   :opt-un [:timers/note]))
