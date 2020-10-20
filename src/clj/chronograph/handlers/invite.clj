(ns chronograph.handlers.invite
  (:require [next.jdbc :as jdbc]
            [chronograph.db.core :as db]
            [chronograph.domain.invite :as invite]
            [ring.util.response :as response]
            [chronograph.domain.organization :as organization]))

(defn accept
  [{{:users/keys [email id]} :user
    {:keys [slug]}        :params}]
  (jdbc/with-transaction [tx db/datasource]
    (if-let [{invite-id :invites/id} (invite/find-by-org-slug-and-email tx slug email)]
      (-> (response/response (invite/accept! tx invite-id id))
          (response/status 200))
      (response/not-found {:error "Not found"}))))

(defn reject
  [{{:users/keys [email]} :user
    {:keys [slug]}        :params}]
  (jdbc/with-transaction [tx db/datasource]
    (if-let [{invite-id :invites/id} (invite/find-by-org-slug-and-email tx slug email)]
      (-> (response/response (invite/reject! tx invite-id))
          (response/status 200))
      (response/not-found {:error "Not found"}))))

(defn invited-orgs
  [{{email :users/email} :user}]
  (response/response (organization/find-invited db/datasource email)))
