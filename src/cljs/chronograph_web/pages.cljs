(ns chronograph-web.pages
  (:require [re-frame.core :as rf]
            [chronograph-web.subscriptions :as subs]
            [chronograph-web.components.antd :as antd]
            ["@ant-design/icons" :as icons]))

(defn- sider-logo []
  [:div {:style {:margin "16px"}}
   [:> icons/ClockCircleOutlined {:style {:fontSize "32px"}}]])

(defn- user-menu []
  [antd/menu {:mode "inline"}
   (antd/menu-item {:key "1" :icon icons/SettingOutlined} "Settings")
   (antd/menu-divider {:key "2"})
   (antd/menu-item {:key "3" :icon icons/LogoutOutlined} "Logout")])

(defn- user-dropdown [name photo-url]
  [antd/dropdown {:overlay [user-menu]}
   [:span
    [antd/space {:style {:cursor "pointer"}}
     [antd/avatar {:size "large"
                   :src  photo-url}]
     name
     [:> icons/DownOutlined]]]])

(defn- page-header [{:keys [name photo-url]}]
  [antd/layout-header
   [antd/row {:justify "end"
              :align   "middle"}
    [user-dropdown name photo-url]]])

(defn- page-sider []
  [antd/layout-sider
   [sider-logo]
   [antd/menu {:mode "inline"}
    (antd/menu-item {:key "1" :icon icons/ClockCircleOutlined} "Timers")
    (antd/menu-submenu {:title "Admin" :icon icons/TeamOutlined :key "2"}
                       (antd/menu-item {:key "2.1"} "Members")
                       (antd/menu-item {:key "2.2"} "Tasks"))]])

(defn main-page-container
  [& children]
  (let [user-info @(rf/subscribe [::subs/user-info])]
    [antd/layout
     [page-sider]
     [antd/layout
      [page-header user-info]
      (into [antd/layout-content] children)]]))
