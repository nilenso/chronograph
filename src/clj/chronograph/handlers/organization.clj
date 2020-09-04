(ns chronograph.handlers.organization
  (:require [ring.util.response :as response]
            [chronograph.domain.organization :as organization]))

(defn create
  "Any authorized user may create a new organization, provided they submit
  a unique slug. The user who creates an organization is automatically made
  admin of that organization."
  [{{:keys [name slug]} :body
    {:keys [users/id] :as user} :user
    :as request}]
  (if-not user
    (-> (response/response {:error "Unauthorized"})
        (response/status 401))
    (response/response
     (organization/create! {:organizations/name name
                            :organizations/slug slug}
                           id))))
