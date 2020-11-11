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

(defn- duration-display [{:keys [hours minutes]} show-colon?]
  [antd/row {:wrap  false
             :class "timer-duration large-text"}
   [antd/text {:strong "true"} hours]
   [antd/text {:class  (str "timer-duration-colon "
                            (when-not show-colon?
                              "invisible"))
               :strong "true"} ":"]
   [antd/text {:strong "true"} (left-pad 2 "0" (str minutes))]])

(defn- running? [{:keys [time-spans]}]
  (and (not-empty time-spans)
       (not (:stopped-at (last time-spans)))))

(defn timer [{:keys [task note] :as timer} on-start on-stop on-delete]
  (r/with-let [current-time            (r/atom (js/Date.))
               show-colon              (r/atom true)
               timer-interval-id       (js/setInterval #(reset! current-time (js/Date.)) 1000)
               colon-blink-interval-id (js/setInterval #(swap! show-colon not) 500)
               show-delete-popconfirm? (r/atom false)]
    [:<>
     [antd/row {:class  (str "timer " (when (running? timer)
                                        "timer-running"))
                :align  "middle"
                :wrap   false
                :gutter 16}
      [antd/col
       [duration-display
        (time/timer-duration timer @current-time)
        (or (not (running? timer))
            @show-colon)]]

      [antd/col
       [antd/row [antd/text {:class  "large-text"
                             :strong "true"} (:name task)]]
       (when (and note
                  (not= "" note))
         [antd/row [antd/text {:type "secondary"} note]])
       [antd/row
        [antd/space
         (if (running? timer)
           [antd/button {:type    "primary"
                         :icon    icons/PauseCircleFilled
                         :onClick #(rf/dispatch on-stop)}
            "Stop"]
           [antd/button {:icon    icons/PlayCircleFilled
                         :onClick #(rf/dispatch on-start)}
            "Start"])
         [antd/popconfirm {:title           "Are you sure you want to delete this timer?"
                           :visible         @show-delete-popconfirm?
                           :onVisibleChange #(reset! show-delete-popconfirm? %)
                           :okText          "Yes"
                           :cancelText      "No"
                           :placement       "bottom"
                           :onConfirm       #(rf/dispatch on-delete)}
          [antd/button {:type    "danger"
                        :icon    icons/DeleteOutlined
                        :onClick #(reset! show-delete-popconfirm? true)}
           "Delete"]]]]]]]
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
                 :class     "timer create-timer-widget"}
     [antd/select (assoc (get-select-attributes :task {:value-fn identity})
                         :class "task-select-dropdown")
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
