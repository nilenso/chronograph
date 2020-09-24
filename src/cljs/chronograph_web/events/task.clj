(ns chronograph-web.components.tasks
  (:require [chronograph-web.subscriptions :as subs]
            [re-frame.core :as rf]))

(defn new-task-form []
  (let [new-task @(rf/subscribe [::subs/create-task-form])]))
