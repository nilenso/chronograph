(ns chronograph.factories
  (:require [chronograph.domain.user :as user]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [chronograph.domain.organization :as organization]))

(defn create-user []
  (let [{:keys [users/id users/name users/email users/photo-url]} (gen/generate (s/gen :users/user))]
    (user/find-or-create-google-user! (gen/generate (s/gen :users/google-id))
                                      name
                                      email
                                      photo-url)))

(defn create-organization [owner-id]
  (organization/create!
   (gen/generate (s/gen :organizations/create-params))
   owner-id))
