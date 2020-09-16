(ns chronograph.factories
  (:require [chronograph.domain.user :as user]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [chronograph.domain.task :as task]
            [chronograph.domain.organization :as organization]))

(defn create-user []
  (let [{:keys [users/name users/email users/photo-url]} (gen/generate (s/gen :users/user))]
    (user/find-or-create-google-user! (gen/generate (s/gen :users/google-id))
                                      name
                                      email
                                      photo-url)))

(defn create-organization [owner-id]
  (organization/create!
   (gen/generate (s/gen :organizations/create-params))
   owner-id))

(defn create-task [organization-id]
  (task/create
    (merge
      (gen/generate (s/gen :tasks/create-params-handler))
      {:organization-id organization-id})))
