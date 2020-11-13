(ns chronograph-web.db)

(def default-db {:user {:signin-state :fetching-data}
                 :page {:handler :root}})

(defn normalize-by
  "Like group-by, except f is expected to be unique
  for each value in the collection. Returns a map of
  keys returned by f to the values in the collection."
  [f coll]
  (zipmap (map f coll) coll))

(def conj-to-set (fnil conj #{}))

(defn add-to-set-in
  "Adds v to a set in the db at the given path."
  [db path v]
  (update-in db
             path
             conj-to-set
             v))

(defn set-page-key [db page-key]
  (assoc-in db [:page-key] page-key))

(defn current-page-key
  [db]
  (get-in db [:page-key]))

;; Page state functions
;; Use these functions to get and set state which is specific to
;; the current page.

(defn get-in-page-state
  [db path]
  (get-in db (concat [:page-state (current-page-key db)] path)))

(defn set-in-page-state
  [db path value]
  (assoc-in db (concat [:page-state (current-page-key db)] path) value))

(defn update-in-page-state
  [db path f & args]
  (apply update-in
         db
         (concat [:page-state (current-page-key db)] path)
         f
         args))

(defn clear-page-state
  [db]
  (update db :page-state dissoc (current-page-key db)))
