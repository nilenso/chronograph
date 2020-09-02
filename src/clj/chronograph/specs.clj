(ns chronograph.specs
  (:require [clojure.spec.alpha :as s]
            [chronograph.domain.acl :as acl]))

;; Users

(s/def :users/id int?)
(s/def :users/name string?)
(s/def :users/email string?)
(s/def :users/photo-url string?)

(s/def :users/user (s/keys :req [:users/id :users/name :users/email]
                           :opt [:users/photo-url]))

(s/def :users/google-id string?)

;; Organizations
(s/def :organizations/name string?)
(s/def :organizations/slug string?)
(s/def :organizations/id int?)

(s/def :organizations/create-params (s/keys :req [:organizations/name :organizations/slug]))
(s/def :organizations/organization (s/keys :req [:organizations/id
                                                 :organizations/name
                                                 :organizations/slug]))

;; ACLs
(s/def :acls/role #{acl/admin acl/member})
(s/def :acls/user-id :users/id)
(s/def :acls/organization-id :organizations/id)

(s/def :acls/acl (s/keys :req [:acls/user-id :acls/organization-id :acls/role]))
