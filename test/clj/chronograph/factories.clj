(ns chronograph.factories
  (:require [chronograph.db.core :as db]
            [chronograph.domain.user :as user]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [chronograph.domain.task :as task]
            [chronograph.domain.organization :as organization]
            [next.jdbc :refer [with-transaction]]))

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
                          (gen/generate (s/gen :organizations/create-params))
                          owner-id)))

(defn create-task [organization-id]
  (with-transaction [tx db/datasource]
    (task/create tx
                 (merge
                  (gen/generate (s/gen :tasks/create-params-handler))
                  {:organization-id organization-id}))))
