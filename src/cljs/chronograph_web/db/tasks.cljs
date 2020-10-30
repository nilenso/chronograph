(ns chronograph-web.db.tasks)

(defn find-by-id [db task-id]
  (get-in db [:tasks task-id]))
