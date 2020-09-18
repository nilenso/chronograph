(ns chronograph.specs
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]))

;; Users

(s/def :users/id int?)
(s/def :users/name string?)
(s/def :users/email string?)
(s/def :users/photo-url string?)

(s/def :users/user (s/keys :req [:users/id :users/name :users/email]
                           :opt [:users/photo-url]))

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

(s/def :organizations/id int?)

(s/def :organizations/create-params (s/keys :req [:organizations/name :organizations/slug]))
(s/def :organizations/create-params-un (s/keys :req-un [:organizations/name :organizations/slug]))
(s/def :organizations/organization (s/keys :req [:organizations/id
                                                 :organizations/name
                                                 :organizations/slug]))

;; ACLs
(s/def :acls/role #{"admin" "member"})
(s/def :acls/user-id :users/id)
(s/def :acls/organization-id :organizations/id)

(s/def :acls/acl (s/keys :req [:acls/user-id :acls/organization-id :acls/role]))
