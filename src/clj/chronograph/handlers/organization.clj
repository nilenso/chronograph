(ns chronograph.handlers.organization
  (:require [chronograph.db.core :as db]
            [chronograph.domain.organization :as organization]
            [clojure.spec.alpha :as s]
            [next.jdbc :as jdbc]
            [ring.util.response :as response]))

(defn index [{:keys [user]}]
  (jdbc/with-transaction [tx db/datasource]
    (-> (organization/for-user tx user)
        response/response)))

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
    (jdbc/with-transaction [tx db/datasource]
      (-> (organization/create! tx
                                {:organizations/name name :organizations/slug slug}
                                id)
          response/response))))

(defn find-one
  "Any user may query details about the requested organization, as long as they
  belong to the organization."
  [{{:keys [slug]} :params
    {:users/keys [id]} :user
    :as _request}]
  (if-not (s/valid? :organizations/slug slug)
    (response/bad-request
     {:error "Bad slug"})
    (jdbc/with-transaction [tx db/datasource]
      (if-let [organization (organization/find-if-authorized tx slug id)]
        (-> organization
            response/response)
        (response/not-found
         {:error "Not found"})))))
