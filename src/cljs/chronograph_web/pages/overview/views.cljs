(ns chronograph-web.pages.overview.views
  (:require [re-frame.core :as rf]
            [chronograph-web.pages.overview.events :as overview-events]
            [chronograph-web.pages.overview.subscriptions :as overview-subs]
            [chronograph-web.components.antd :as antd]
            [chronograph.specs]
            [chronograph-web.page-container.views :as page-container]
            [chronograph-web.utils.time :as time])
  (:import [goog.date Date]))

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
                                  [antd/button {:onClick #(rf/dispatch [::overview-events/accept-invite id])
                                                :type    "link"}
                                   "Accept"]
                                  [antd/button {:onClick #(rf/dispatch [::overview-events/reject-invite id])
                                                :danger  true
                                                :type    "link"}
                                   "Decline"]]]])
                 :dataSource organizations}]]))

(defn timer-list [ds]
  [antd/list {:dataSource ds
              :grid {:gutter 64
                     :xl 4
                     :xxl 4}
              :renderItem (fn [{:keys [name duration]}]
                            [antd/list-item {:key name
                                             :actions [[antd/button {:type "link"} "Start"]]}
                             [antd/list-item-meta {:title [name]}]
                             duration])}])

(defn landing-page [_]
  (fn [_]
    (let [invited-organizations @(rf/subscribe [::overview-subs/invites])
          timers @(rf/subscribe [::overview-subs/timers (time/current-calendar-date)])]
      [page-container/org-scoped-page-container
       [antd/page-header {:title "Overview"}
        [invited-organizations-list invited-organizations]
        (when (not-empty invited-organizations)
          [antd/divider])
        [timer-list (into [] (map #(do {:duration (time/timer-duration %)
                                        :name (get-in % [:task :name])}) timers))]]])))

