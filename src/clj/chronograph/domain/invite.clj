(ns chronograph.domain.invite
  (:require [chronograph.db.invite :as db-invite]
            [chronograph.domain.acl :as acl]
            [chronograph.domain.user :as user]))

(defn- email-belongs-to-org?
  [tx org-id email]
  (when-let [user (user/find-by-email tx email)]
    (acl/belongs-to-org? tx (:users/id user) org-id)))

(defn find-or-create!
  [tx org-id email]
  (if-not (email-belongs-to-org? tx org-id email)
    (or (db-invite/find-by-org-id-and-email tx org-id email)
        (db-invite/create! tx org-id email))
    ::error-user-belongs-to-org))

(def find-by-org-id db-invite/find-by-org-id)

(defn find-by-org-slug-and-email
  [tx org-slug email]
  (db-invite/find-by-org-slug-and-email tx org-slug email))

(defn accept!
  [tx invite-id user-id]
  (let [{:invites/keys [organization-id]
         :as           deleted-invite} (db-invite/delete! tx {:id invite-id})]
    (acl/create! tx #:acls{:user-id         user-id
                           :organization-id organization-id
                           :role            acl/member})
    deleted-invite))

(defn reject!
  [tx invite-id]
  (db-invite/delete! tx {:id invite-id}))
