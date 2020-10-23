(ns chronograph-web.pages
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [chronograph-web.events.organization :as org-events]
            [chronograph-web.subscriptions :as subs]
            ["antd" :as antd-raw]
            [chronograph-web.components.antd :as antd]
            ["@ant-design/icons" :as icons]))

(defn- header-logo []
  [:> icons/ClockCircleOutlined {:style {:color "wheat" :fontSize "32px"}}])

(defn- user-menu []
  [antd/menu {:mode "inline"}
   (antd/menu-item {:key "1" :icon (r/create-element icons/SettingOutlined)} "Settings")
   (antd/menu-divider {:key "2"})
   (antd/menu-item {:key "3" :icon (r/create-element icons/LogoutOutlined)} "Logout")])

(defn- user-dropdown [name photo-url]
  [antd/dropdown {:overlay (r/as-element [user-menu])}
   [:span {:style {:color "wheat"}}
    [antd/space
     [antd/avatar {:size "large"
                   :src  photo-url}]
     name]]])

(defn- user-header [{:keys [name photo-url]}]
  [antd/layout-header
   [antd/row {:justify "space-between"
              :align   "middle"}
    [header-logo]
    [user-dropdown name photo-url]]])

(defn with-user-header []
  (fn [_]
    (let [children  (r/children (r/current-component))
          user-info @(rf/subscribe [::subs/user-info])]
      [antd/layout
       [user-header user-info]
       (into [antd/layout-content {:style {:padding "0 50px"}}] children)])))

;; Unused code. But, should be useful soon.
(defn- organization-select-menu []
  [:> antd-raw/Menu {:mode "inline"}
   [:> antd-raw/Menu.Item {:key "1"} "nilenso"]
   [:> antd-raw/Menu.Item {:key "2"} "client"]
   [:> antd-raw/Menu.Divider {:key "3"}]
   [:> antd-raw/Menu.Item {:key "4" :icon (r/create-element icons/PlusOutlined)} "New"]])

(defn- organization-sider []
  [:> antd-raw/Layout.Sider
   [:> antd-raw/Menu {:mode "inline"}
    [:> antd-raw/Menu.Item {:key "1" :icon (r/create-element icons/ClockCircleOutlined)} "Timers"]
    [:> antd-raw/Menu.SubMenu {:title "Admin" :icon (r/create-element icons/TeamOutlined) :key "2"}
     [:> antd-raw/Menu.Item {:key "2.1"} "Members"]
     [:> antd-raw/Menu.Item {:key "2.2"} "Tasks"]]]])

(defn- user-and-organization-header []
  [:> antd-raw/Layout.Header
   [:> antd-raw/Row {:justify "space-between"}
    [header-logo]
    [:> antd-raw/Dropdown {:trigger "click"
                           :overlay (r/create-element (r/reactify-component organization-select-menu))}
     [:> icons/DownOutlined {:style {:color "wheat" :fontSize "32px"}}]]]])

(defn with-organization-sidebar [_]
  (rf/dispatch [::org-events/fetch-organizations])
  (fn [_]
    (let [children (r/children (r/current-component))]
      [:<>
       [:> antd-raw/Layout
        [user-and-organization-header]
        [:> antd-raw/Layout
         [organization-sider]
         (into [:> antd-raw/Layout.Content] children)]]])))
