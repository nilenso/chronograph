(ns chronograph.factories
  (:require [chronograph.db.core :as db]
            [chronograph.domain.user :as user]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [chronograph.domain.acl :as acl]
            [chronograph.domain.task :as task]
            [chronograph.domain.organization :as organization]
            [next.jdbc :refer [with-transaction]]
            [chronograph.domain.timer :as timer])
  (:import [java.time LocalDate]))

(defn create-user []
  (let [{:keys [users/name users/email users/photo-url]} (gen/generate (s/gen :users/user))]
    (with-transaction [tx db/datasource]
      (user/find-or-create-google-user! tx
                                        (gen/generate (s/gen :users/google-id))
                                        name
                                        email
                                        photo-url))))

(defn create-organization [owner-id]
  (with-transaction [tx db/datasource]
    (organization/create! tx
                          (gen/generate (s/gen :organizations/organization))
                          owner-id)))

(defn create-user-and-organization []
  (let [{user-id :users/id} (create-user)]
    (create-organization user-id)))

(defn create-task [organization]
  (with-transaction [tx db/datasource]
    (task/create tx
                 (gen/generate (s/gen :tasks/task))
                 organization)))

(defn create-acl [user organization role]
  (with-transaction [tx db/datasource]
    (acl/create! tx
                 {:acls/user-id (:users/id user)
                  :acls/organization-id (:organizations/id organization)
                  :acls/role role})))

(defn create-timer
  ([organization-id timer]
   (with-transaction [tx db/datasource]
     (timer/create! tx
                    organization-id
                    (merge #:timers{:recorded-for (LocalDate/parse "2020-01-14")
                                    :note (gen/generate (s/gen :timers/note))}
                           timer))))
  ([organization-id user-id task-id]
   (with-transaction [tx db/datasource]
     (timer/create! tx
                    organization-id
                    #:timers{:user-id user-id
                             :task-id task-id
                             :recorded-for (LocalDate/parse "2020-01-14")
                             :note (gen/generate (s/gen :timers/note))}))))
