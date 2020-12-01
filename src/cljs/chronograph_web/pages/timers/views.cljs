(ns chronograph-web.pages.timers.views
  (:require [re-frame.core :as rf]
            [chronograph-web.pages.timers.events :as timers-events]
            [chronograph-web.components.invites :as invites]
            [chronograph-web.pages.timers.subscriptions :as timers-subs]
            [chronograph-web.components.antd :as antd]
            [chronograph.specs]
            [chronograph-web.page-container.views :as page-container]
            [chronograph-web.events.organization-invites :as org-invites-events]
            ["@ant-design/icons" :as icons]
            [reagent.core :as r]
            [chronograph-web.api-client :as api]
            [chronograph-web.components.form :as form]
            [chronograph-web.utils.time :as time]
            [chronograph.utils.string :as ustring]
            [clojure.string :as string]
            [cljs.spec.alpha :as s]))

(defn- duration-display [{:keys [hours minutes]} show-colon?]
  [antd/row {:wrap  false
             :class "timer-duration large-text"}
   [antd/text {:strong "true"} hours]
   [antd/text {:class  (str "timer-duration-colon "
                            (when-not show-colon?
                              "invisible"))
               :strong "true"} ":"]
   [antd/text {:strong "true"} (ustring/left-pad 2 "0" (str minutes))]])

(defn- running? [{:keys [time-spans]}]
  (and (not-empty time-spans)
       (not (:stopped-at (last time-spans)))))

(defn- task-option [{:keys [id name]}]
  (with-meta (antd/option {:value id :key id} name)
    {:key id}))

(defn- task-chooser [attributes]
  (let [tasks @(rf/subscribe [::timers-subs/tasks])]
    [antd/select (update attributes :class str " task-select-dropdown")
     (map task-option tasks)]))

(defn- seconds-from-duration-string [s]
  (let [[hours mins] (map js/parseInt (string/split s #":"))]
    (+ (* hours 60 60)
       (* mins 60))))

(defn- duration-string? [s]
  (when-not (string/blank? s)
    (let [[hours-str mins-str] (string/split s #":")
          hours (js/parseInt hours-str)
          mins  (js/parseInt mins-str)]
      (and (re-matches #"\d?\d:\d\d" s)
           (int? hours)
           (<= 0 hours)
           (int? mins)
           (<= 0 mins 59)))))

(s/def :pages.timers.views/duration-string duration-string?)

(defn- edit-timer-button-and-form
  [timer-id]
  (r/with-let [show-edit-modal? (rf/subscribe [::timers-subs/showing-edit-timer-modal? timer-id])
               close-modal      #(rf/dispatch [::timers-events/dismiss-edit-timer-modal timer-id])
               {::form/keys [get-submit-attributes
                             get-input-attributes
                             get-select-attributes]}
               (form/form {:form-key        [::timers-events/edit-timer-form timer-id]
                           :specs           {:duration-in-hour-mins :pages.timers.views/duration-string
                                             :task-id               :tasks/id}
                           :request-builder (fn [{:keys [duration-in-hour-mins] :as edit-data}]
                                              (api/edit-timer timer-id
                                                              (-> edit-data
                                                                  (dissoc :duration-in-hour-mins)
                                                                  (assoc :duration-in-secs (seconds-from-duration-string duration-in-hour-mins)))
                                                              [::timers-events/edit-timer-succeeded timer-id]
                                                              [::timers-events/edit-timer-failed]))})]
    [:<>
     [antd/button {:icon     icons/EditOutlined
                   :on-click #(rf/dispatch [::timers-events/show-edit-timer-modal timer-id])}]
     [antd/modal {:visible  @show-edit-modal?
                  :title    "Edit Timer"
                  :onCancel close-modal
                  :footer   [antd/space
                             [antd/button {:onClick close-modal} "Cancel"]
                             [antd/button (get-submit-attributes) "Submit"]]}
      [antd/space {:direction "vertical"
                   :style     {:width "100%"}}
       [task-chooser (get-select-attributes :task-id)]
       [antd/text-area (get-input-attributes :note)]
       [antd/input (assoc (get-input-attributes :duration-in-hour-mins)
                          :placeholder "Time duration for eg. 1:26")]]]]))

(defn timer-display [{:keys [task note id] :as timer}]
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
                         :onClick #(rf/dispatch [::timers-events/stop-timer id])}
            "Stop"]
           [antd/button {:icon    icons/PlayCircleFilled
                         :onClick #(rf/dispatch [::timers-events/start-timer id])}
            "Start"])
         [edit-timer-button-and-form
          id]
         [antd/popconfirm {:title           "Are you sure you want to delete this timer?"
                           :visible         @show-delete-popconfirm?
                           :onVisibleChange #(reset! show-delete-popconfirm? %)
                           :okText          "Yes"
                           :cancelText      "No"
                           :placement       "bottom"
                           :onConfirm       #(rf/dispatch [::timers-events/delete-timer id])}
          [antd/button {:type    "danger"
                        :icon    icons/DeleteOutlined
                        :onClick #(reset! show-delete-popconfirm? true)}]]]]]]]
    (finally (js/clearInterval timer-interval-id)
             (js/clearInterval colon-blink-interval-id))))

(defn create-timer-widget [on-cancel-event create-timer-succeeded-event create-timer-failed-event]
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
     [task-chooser (get-select-attributes :task)]
     [antd/text-area (merge (get-input-attributes :note)
                            {:placeholder "Note"
                             :rows        2})]
     [antd/space
      [antd/button (assoc (get-submit-attributes) :icon icons/PlayCircleFilled)
       "Start"]
      [antd/button
       {:onClick #(rf/dispatch on-cancel-event)}
       "Cancel"]]]))

(defn timer-list [ds]
  [antd/list {:dataSource ds
              :grid       {:gutter 64}
              :renderItem (fn [{:keys [id state] :as timer}]
                            (if (= "creating" state)
                              [antd/list-item {:key "creating"}
                               [create-timer-widget
                                [::timers-events/dismiss-create-timer-widget]
                                [::timers-events/create-timer-succeeded]
                                [::timers-events/create-timer-failed]]]
                              [antd/list-item {:key   id
                                               :style {:height "100%"}}
                               [timer-display
                                timer]]))}])

(defn actions [showing-create-timer-widget?]
  [:<>
   [antd/button
    {:icon    icons/ArrowLeftOutlined
     :title   "Previous day"
     :onClick #(rf/dispatch [::timers-events/modify-selected-date -1])}]
   [antd/date-picker {:value    @(rf/subscribe [::timers-subs/selected-date])
                      :onChange #(rf/dispatch [::timers-events/calendar-select-date %])}]
   [antd/button
    {:icon    icons/ArrowRightOutlined
     :title   "Next day"
     :onClick #(rf/dispatch [::timers-events/modify-selected-date 1])}]
   (when-not showing-create-timer-widget?
     [antd/button
      {:type    "primary"
       :icon    icons/PlusOutlined
       :onClick #(rf/dispatch [::timers-events/show-create-timer-widget])}
      "New Timer"])])

(defn landing-page [_]
  (let [invited-organizations        @(rf/subscribe [::timers-subs/invites])
        showing-create-timer-widget? @(rf/subscribe [::timers-subs/showing-create-timer-widget?])
        timers                       @(rf/subscribe [::timers-subs/current-organization-timers])]
    [page-container/org-scoped-page-container
     [antd/page-header {:title "Timers"
                        :extra [actions showing-create-timer-widget?]}
      [invites/invited-organizations-list
       invited-organizations
       #(rf/dispatch [::org-invites-events/accept-invite %])
       #(rf/dispatch [::org-invites-events/reject-invite %])]
      (when (not-empty invited-organizations)
        [antd/divider])
      [timer-list (if showing-create-timer-widget?
                    (cons {:state "creating"} timers)
                    timers)]]]))

