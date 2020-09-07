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
  (cond
    (not user)
    (-> (response/response {:error "Unauthorized"})
        (response/status 401))

    (not (s/valid? :organizations/create-params-handler
                   body))
    (response/bad-request {:error "Bad name or slug."})

    :else
    (response/response
     (organization/create! {:organizations/name name
                            :organizations/slug slug}
                           id))))
