(ns chronograph.specs
  (:require [clojure.spec.alpha :as s]))

;; Users

(s/def :users/id int?)
(s/def :users/name string?)
(s/def :users/email string?)
(s/def :users/photo-url string?)

(s/def :users/user (s/keys :req [:users/id :users/name :users/email]
                           :opt [:users/photo-url]))

(s/def :users/google-id string?)
