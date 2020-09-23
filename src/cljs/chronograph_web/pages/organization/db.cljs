(ns chronograph-web.pages.organization.db)

;; TODO: Move this
(defn add-to-set-in
  "Adds v to a set in the db at the given path."
  [db path v]
  (update-in db
             path
             (fnil conj #{})
             v))

(def page-name :organization-show)

(defn get-from-add-member-form
  [db form-key]
  (get-in db [:page-state page-name :add-member-form form-key]))

(defn set-in-add-member-form
  [db form-key value]
  (assoc-in db [:page-state page-name :add-member-form form-key] value))

(defn slug
  [db]
  (get-in db [:page :route-params :slug]))

(defn org-by-slug
  [db slug]
  (get-in db [:organizations slug]))

(defn current-org
  [db]
  (let [org-slug (slug db)]
    (org-by-slug db org-slug)))

(defn add-invited-member
  [db org-id email]
  (add-to-set-in db
                 [:invited-members org-id]
                 {:organization-id org-id
                  :email           email}))

(defn add-invited-members
  [db members]
  (reduce (fn [db {:keys [organization-id email]}]
            (add-invited-member db organization-id email))
          db
          members))

(defn current-org-id
  [db]
  (:id (current-org db)))

(defn add-joined-member
  [db member]
  (add-to-set-in db
                 [:joined-members (current-org-id db)]
                 member))

(defn add-joined-members
  [db members]
  (reduce add-joined-member
          db
          members))

(defn get-invited-members
  [db]
  (get-in db [:invited-members (:id (current-org db))]))

(defn get-joined-members
  [db]
  (get-in db [:joined-members (current-org-id db)]))

;; TODO: Extract these out
(defn report-error
  [db error]
  (update-in db
             [:page-state page-name :errors]
             (fnil conj #{})
             error))

(defn remove-error
  [db error]
  (update-in db
             [:page-state page-name :errors]
             disj
             error))

(defn get-errors
  [db]
  (get-in db [:page-state page-name :errors]))
