(ns chronograph-web.components.timer
  (:require [chronograph-web.utils.time :as time]
            [chronograph-web.components.antd :as antd]
            ["@ant-design/icons" :as icons]))

(defn- left-pad [n val s]
  (if (>= (count s) n)
    s
    (->> s
         (concat (repeat (- n (count s)) val))
         (apply str))))

(defn- format-duration [{:keys [hours minutes]}]
  (str hours ":" (left-pad 2 "0" (str minutes))))

(defn- running? [{:keys [time-spans]}]
  (and (not-empty time-spans)
       (not (:stopped-at (last time-spans)))))

(defn timer [{:keys [task] :as timer}]
  [:<>
   [antd/row {:class  (str "timer " (when (running? timer)
                                      "timer-running"))
              :align  "middle"
              :wrap   false
              :gutter 16}
    [antd/col
     [:span {:class "timer-duration"} (format-duration (time/timer-duration timer))]]

    [antd/col
     [antd/row [:span (:name task)]]
     [antd/row
      (if (running? timer)
        [antd/button {:type "primary"
                      :icon icons/PauseCircleFilled}
         "Stop"]
        [antd/button {:icon icons/PlayCircleFilled}
         "Start"])]]]])

