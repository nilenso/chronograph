(ns chronograph.db.google-profile
  (:require [chronograph.db.core :as db]))

(defn create! [tx {:google-profiles/keys [google-id]}]
  (db/create! :google-profiles
              tx
              {:google-id google-id}))
