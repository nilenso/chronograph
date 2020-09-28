(ns chronograph.handlers.organization
  (:require [ring.util.response :as response]
            [chronograph.domain.organization :as organization]
            [chronograph.domain.invite :as invite]
            [clojure.spec.alpha :as s]
            [next.jdbc :as jdbc]
            [chronograph.db.core :as db]
            [chronograph.domain.acl :as acl]))

(defn create
  "Any authorized user may create a new organization, provided they submit
  a unique slug. The user who creates an organization is automatically made
  admin of that organization."
  [{{:keys [name slug] :as body} :body
    {:keys [users/id]} :user
    :as _request}]
  (if-not (s/valid? :organizations/create-params-un body)
    (response/bad-request
     {:error "Bad name or slug."})
    (-> (organization/create! {:organizations/name name :organizations/slug slug}
                              id)
        response/response)))

(defn find-one
  "Any user may query details about the requested organization, as long as they
  belong to the organization."
  [{{:keys [slug]} :params
    {:users/keys [id]} :user
    :as _request}]
  (if-not (s/valid? :organizations/slug slug)
    (response/bad-request
     {:error "Bad slug"})
    (if-let [organization (organization/find-if-authorized slug id)]
      (-> organization
          response/response)
      (response/not-found
       {:error "Not found"}))))

(defn invite
  [{{:keys [slug]} :params
    {user-id :users/id} :user
    {:keys [email]} :body
    :as _request}]
  (jdbc/with-transaction [tx db/datasource]
    (let [{org-id :organizations/id} (organization/find-by-slug tx slug)]
      (cond
        (not org-id)                          (response/not-found {:error "Not found"})
        (not (s/valid? :invites/email email)) (response/bad-request {:error "Invalid email"})
        (not (acl/admin? tx user-id org-id))  (-> (response/response {:error "Forbidden"})
                                                  (response/status 403))
        :else                                 (response/response
                                               (invite/find-or-create! tx org-id email))))))

(defn show-members
  [{{:keys [slug]} :params
    {user-id :users/id} :user
    :as _request}]
  (jdbc/with-transaction [tx db/datasource]
    (let [{:organizations/keys [id]} (organization/find-by-slug tx slug)]
      (cond
        (not id)                         (response/not-found {:error "Not found"})
        (not (acl/admin? tx user-id id)) (-> (response/response {:error "Forbidden"})
                                             (response/status 403))
        :else                            (response/response {:joined (organization/members tx id)
                                                             :invited (invite/find-by-org-id tx id)})))))


