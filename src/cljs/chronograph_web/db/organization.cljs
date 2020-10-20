(ns chronograph-web.db.organization)

(defn org-by-slug
  [db slug]
  (get-in db [:organizations slug]))

(defn add-org
  [db {:keys [slug] :as organization}]
  (assoc-in db [:organizations slug] organization))
