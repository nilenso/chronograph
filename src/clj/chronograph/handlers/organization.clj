(ns chronograph.handlers.organization
  (:require [ring.util.response :as response]
            [chronograph.domain.organization :as organization]
            [clojure.spec.alpha :as s]))

(defn create
  "Any authorized user may create a new organization, provided they submit
  a unique slug. The user who creates an organization is automatically made
  admin of that organization."
  [{{:keys [name slug] :as body} :body
    {:keys [users/id] :as user} :user
    :as request}]
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
    :as request}]
  (if-not (s/valid? :organizations/slug slug)
    (response/bad-request
     {:error "Bad slug"})
    (if-let [organization (organization/find-if-authorized slug id)]
      (-> organization
          response/response)
      (response/not-found
       {:error "Not found"}))))
