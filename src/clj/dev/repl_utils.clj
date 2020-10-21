(ns dev.repl-utils
  (:require [mount.core :as mount]
            [chronograph.server :as server]
            [chronograph.core :as core]
            [chronograph.config :as config]
            [chronograph.migrations :as migrations]
            [migratus.core :as migratus]
            [next.jdbc :refer [with-transaction]]
            [chronograph.db.core :as db]
            [chronograph.domain.user :as domain-user] ; do NOT alias as user, else lein repl barfs
            [chronograph.domain.organization :as organization]
            [chronograph.domain.invite :as invite]))

(defn start-app! []
  (core/mount-init!)
  (-> (mount/with-args {:options {:config-file "config/config.dev.edn"}})
      (mount/swap-states {#'server/server {:start #(server/start-server! #'server/handler)
                                           :stop  #(server/server)}})
      mount/start))

(defn restart-app! []
  (mount/stop)
  (start-app!))

(defn load-config!
  ([] (load-config! "config/config.dev.edn"))
  ([config-file]
   (mount/stop #'config/config)
   (mount/start-with-args {:options {:config-file config-file}} #'config/config)))

(defn create-migration [migration-name]
  (migratus/create (migrations/config) migration-name))

(defn setup-org-for-invites-and-invite-self!
  "For REPL use, to help with click-through testing for the invite user flow.

  A user having the invited email must _not_ already be part of the inviting
  organization. Also we should be able to OAuth using an email ID we control.

  Thus, we can setup an organization _not_ owned by us, to which we can
  invite our email."
  [email-id-to-invite]
  (with-transaction [tx db/datasource]
    (let [uuid (str (java.util.UUID/randomUUID))
          owner (domain-user/find-or-create-google-user! tx
                                                         uuid
                                                         (str "Some User " uuid)
                                                         "some-email@nilenso.org"
                                                         "http://my.photo.org/foo.jpg")
          org (organization/create! tx
                                    #:organizations{:name (str "A test organization " uuid)
                                                    :slug (str "test-org-" uuid)}
                                    (:users/id owner))]
      (invite/find-or-create! tx (:organizations/id org) email-id-to-invite))))

(defn remove-user-from-org!
  "For REPL use, when one wants to remove a user from an organization."
  [user-id org-id]
  (db/delete! :acls
              db/datasource
              {:user-id         user-id
               :organization-id org-id}))
