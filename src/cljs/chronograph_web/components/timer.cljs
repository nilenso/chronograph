(ns chronograph-web.components.timer
  (:require [chronograph-web.utils.time :as time]
            [chronograph-web.components.antd :as antd]
            [reagent.core :as r]
            ["@ant-design/icons" :as icons]
            [chronograph-web.components.form :as form]
            [chronograph-web.api-client :as api]
            [re-frame.core :as rf]))

(defn- left-pad [n val s]
  (if (>= (count s) n)
    s
    (->> s
         (concat (repeat (- n (count s)) val))
         (apply str))))

(defn- format-duration [{:keys [hours minutes]} show-colon?]
  [:<>
   [:span hours]
   [:span.timer-duration-colon (if show-colon? ":" "  ")]
   [:span (left-pad 2 "0" (str minutes))]])

(defn- running? [{:keys [time-spans]}]
  (and (not-empty time-spans)
       (not (:stopped-at (last time-spans)))))

(defn timer [{:keys [task] :as timer} on-start on-stop]
  (r/with-let [current-time            (r/atom (js/Date.))
               show-colon              (r/atom true)
               timer-interval-id       (js/setInterval #(reset! current-time (js/Date.)) 1000)
               colon-blink-interval-id (js/setInterval #(swap! show-colon not) 500)]
    [:<>
     [antd/row {:class  (str "timer " (when (running? timer)
                                        "timer-running"))
                :align  "middle"
                :wrap   false
                :gutter 16}
      [antd/col
       [:span {:class "timer-duration"} [format-duration
                                         (time/timer-duration timer @current-time)
                                         (or (not (running? timer))
                                             @show-colon)]]]

      [antd/col
       [antd/row [:span (:name task)]]
       [antd/row
        (if (running? timer)
          [antd/button {:type    "primary"
                        :icon    icons/PauseCircleFilled
                        :onClick #(rf/dispatch on-stop)}
           "Stop"]
          [antd/button {:icon    icons/PlayCircleFilled
                        :onClick #(rf/dispatch on-start)}
           "Start"])]]]]
    (finally (js/clearInterval timer-interval-id)
             (js/clearInterval colon-blink-interval-id))))

(defn- task-option [{:keys [id name]}]
  (with-meta (antd/option {:value id :key id} name)
    {:key id}))

(defn create-timer-widget [tasks on-cancel-event create-timer-succeeded-event create-timer-failed-event]
  (r/with-let [{::form/keys [get-submit-attributes
                             get-input-attributes
                             get-select-attributes]} (form/form {:form-key        ::create-timer-form
                                                                 :request-builder (fn [{:keys [task note]}]
                                                                                    (api/create-and-start-timer
                                                                                     task
                                                                                     note
                                                                                     (time/current-calendar-date)
                                                                                     create-timer-succeeded-event
                                                                                     create-timer-failed-event))})]
    [antd/space {:direction "vertical"
                 :class     "timer"}
     [antd/select (assoc (get-select-attributes :task {:value-fn identity})
                         :style {:min-width "200px"})
      (map task-option tasks)]

     [antd/text-area (merge (get-input-attributes :note)
                            {:placeholder "Note"
                             :rows        2})]
     [antd/space
      [antd/button (assoc (get-submit-attributes) :icon icons/PlayCircleFilled)
       "Start"]
      [antd/button
       {:onClick #(rf/dispatch on-cancel-event)}
       "Cancel"]]]))
