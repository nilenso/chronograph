(ns chronograph-web.pages.timers.views
  (:require [re-frame.core :as rf]
            [chronograph-web.pages.timers.events :as timers-events]
            [chronograph-web.components.invites :as invites]
            [chronograph-web.pages.timers.subscriptions :as timers-subs]
            [chronograph-web.components.antd :as antd]
            [chronograph.specs]
            [chronograph-web.page-container.views :as page-container]
            [chronograph-web.events.organization-invites :as org-invites-events]
            [chronograph-web.components.timer :as timer-com]
            ["@ant-design/icons" :as icons]))

(defn timer-list [ds]
  (let [tasks @(rf/subscribe [::timers-subs/tasks])]
    [antd/list {:dataSource ds
                :grid       {:gutter 64}
                :renderItem (fn [{:keys [id state] :as timer}]
                              (if (= "creating" state)
                                [antd/list-item {:key "creating"}
                                 [timer-com/create-timer-widget
                                  tasks
                                  [::timers-events/dismiss-create-timer-widget]
                                  [::timers-events/create-timer-succeeded]
                                  [::timers-events/create-timer-failed]]]
                                [antd/list-item {:key id
                                                 :style {:height "100%"}}
                                 [timer-com/timer
                                  timer
                                  [::timers-events/start-timer id]
                                  [::timers-events/stop-timer id]
                                  [::timers-events/delete-timer id]]]))}]))

(defn actions [showing-create-timer-widget?]
  [:<>
   [antd/button
    {:icon    icons/ArrowLeftOutlined
     :onClick #(rf/dispatch [::timers-events/modify-selected-date -1])}]
   [antd/button
    {:icon    icons/ArrowRightOutlined
     :onClick #(rf/dispatch [::timers-events/modify-selected-date 1])}]
   [antd/date-picker {:value    @(rf/subscribe [::timers-subs/selected-date])
                      :onChange #(rf/dispatch [::timers-events/calendar-select-date %])}]
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

