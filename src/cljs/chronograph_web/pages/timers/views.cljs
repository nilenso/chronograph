(ns chronograph-web.pages.timers.views
  (:require [re-frame.core :as rf]
            [chronograph-web.pages.timers.events :as timers-events]
            [chronograph-web.pages.timers.subscriptions :as timers-subs]
            [chronograph-web.components.antd :as antd]
            [chronograph.specs]
            [chronograph-web.page-container.views :as page-container]
            [chronograph-web.components.timer :as timer-com]
            [chronograph-web.utils.time :as time]
            ["@ant-design/icons" :as icons]))

(defn- invited-organizations-list
  [organizations]
  (when (not-empty organizations)
    [:<>
     [antd/title {:level 4} "My Invitations"]
     [antd/list {:renderItem (fn [{:keys [name id] :as _org}]
                               [antd/list-item {}
                                [antd/row {:align "middle"}
                                 [antd/col {:flex 3} name]
                                 [antd/col {:flex 2}
                                  [antd/button {:onClick #(rf/dispatch [::timers-events/accept-invite id])
                                                :type    "link"}
                                   "Accept"]
                                  [antd/button {:onClick #(rf/dispatch [::timers-events/reject-invite id])
                                                :danger  true
                                                :type    "link"}
                                   "Decline"]]]])
                 :dataSource organizations}]]))

(defn timer-list [ds]
  [antd/list {:dataSource ds
              :grid       {:gutter 64}
              :renderItem (fn [{:keys [id state] :as timer}]
                            (if (= "creating" state)
                              [antd/list-item {:key "creating"}
                               [timer-com/create-timer-widget
                                [::timers-events/dismiss-create-timer-widget]
                                [::timers-events/create-timer-succeeded]
                                [::timers-events/create-timer-failed]]]
                              [antd/list-item {:key id}
                               [timer-com/timer
                                timer
                                [::timers-events/start-timer id]
                                [::timers-events/stop-timer id]]]))}])

(defn landing-page [_]
  (let [invited-organizations        @(rf/subscribe [::timers-subs/invites])
        showing-create-timer-widget? @(rf/subscribe [::timers-subs/showing-create-timer-widget?])
        timers                       @(rf/subscribe [::timers-subs/current-organization-timers
                                                     (time/current-calendar-date)])]
    [page-container/org-scoped-page-container
     [antd/page-header {:title "Timers"
                        :extra (when-not showing-create-timer-widget?
                                 [antd/button
                                  {:type    "primary"
                                   :icon    icons/PlusOutlined
                                   :onClick #(rf/dispatch [::timers-events/show-create-timer-widget])}
                                  "New Timer"])}
      [invited-organizations-list invited-organizations]
      (when (not-empty invited-organizations)
        [antd/divider])
      [timer-list (if showing-create-timer-widget?
                    (cons {:state "creating"} timers)
                    timers)]]]))

