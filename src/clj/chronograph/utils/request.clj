(ns chronograph.utils.request
  (:require [chronograph.db.core :as db]
            [chronograph.db.organization :as org-db]))

(defn current-organization [tx {:keys [body params] :as request}]
  (let [slug (or (:slug body) (:slug params))
        id (or (:organization-id body) (:organization-id params))]
    (or (org-db/find tx {:slug slug})
        (org-db/find tx {:id id}))))
