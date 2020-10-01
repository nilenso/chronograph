(ns chronograph-web.components.tasks
  (:require [chronograph-web.subscriptions :as subs]
            [re-frame.core :as rf]))

(defn new-task-form []
  (let [_new-task @(rf/subscribe [::subs/create-task-form])]
    []))
