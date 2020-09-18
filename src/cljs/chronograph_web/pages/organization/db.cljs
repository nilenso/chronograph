(ns chronograph-web.pages.organization.db)

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

(defn add-invited-member
  [db org-id email]
  (update-in db
             [:invited-members org-id]
             (fnil conj #{})
             {:organization-id org-id
              :email           email}))

(defn add-invited-members
  [db members]
  (reduce (fn [db {:keys [organization-id email]}]
            (add-invited-member db organization-id email))
          db
          members))

(defn add-joined-member
  [db member]
  (assoc-in db
            [:joined-members (:id member)]
            member))

(defn add-joined-members
  [db members]
  (reduce add-joined-member
          db
          members))

(defn current-org
  [db]
  (let [org-slug (slug db)]
    (org-by-slug db org-slug)))

(defn get-invited-members
  [db]
  (get-in db [:invited-members (:id (current-org db))]))

(defn- in-current-org?
  [db {:keys [organization-ids]}]
  (let [org-id (:id (current-org db))]
    (contains? (set organization-ids) org-id)))

(defn get-joined-members
  [db]
  (->> db
       :joined-members
       vals
       (filter (partial in-current-org? db))))

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
