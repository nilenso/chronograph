(ns chronograph-web.db.timers
  (:require [chronograph-web.db :as db]
            [chronograph-web.db.organization :as org-db]))

(defn timers-by-date
  [db date]
  (get-in db [:timers date]))

(defn set-timers [db date timers]
  (assoc-in db [:timers date] timers))
