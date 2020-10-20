(ns chronograph-web.pages
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [chronograph-web.events.organization :as org-events]
            [chronograph-web.subscriptions :as subs]
            ["antd" :as antd]
            ["@ant-design/icons" :as icons]))

(defn- header-logo []
  [:> icons/ClockCircleOutlined {:style {:color "wheat" :fontSize "32px"}}])

(defn- user-menu []
  [:> antd/Menu {:mode "inline"}
   [:> antd/Menu.Item {:key "1" :icon (r/create-element icons/SettingOutlined)} "Settings"]
   [:> antd/Menu.Divider {:key "2"}]
   [:> antd/Menu.Item {:key "3" :icon (r/create-element icons/LogoutOutlined)} "Logout"]])

(defn- user-dropdown []
  [:> antd/Dropdown {:overlay (r/create-element (r/reactify-component user-menu))}
   [:span {:style {:color "wheat"}}
    [:> antd/Avatar {:size "large"
                     :src "profile.png"}]
    "Name"]])

(defn- user-header [{:keys [name]}]
  [:> antd/Layout.Header
   [:> antd/Row {:justify "space-between"}
    [header-logo]
    [user-dropdown]]])

(defn with-user-header []
  (fn [_]
    (let [children (r/children (r/current-component))
          user-info @(rf/subscribe [::subs/user-info])]
      [:<>
       [:> antd/Layout
        [user-header user-info]
        (into [:> antd/Layout.Content {:style {:padding "0 50px"}}] children)]])))

;; Unused code. But, should be useful soon.
(defn- organization-select-menu []
  [:> antd/Menu {:mode "inline"}
   [:> antd/Menu.Item {:key "1"} "nilenso"]
   [:> antd/Menu.Item {:key "2"} "client"]
   [:> antd/Menu.Divider {:key "3"}]
   [:> antd/Menu.Item {:key "4" :icon (r/create-element icons/PlusOutlined)} "New"]])

(defn- organization-sider []
  [:> antd/Layout.Sider
   [:> antd/Menu {:mode "inline"}
    [:> antd/Menu.Item {:key "1" :icon (r/create-element icons/ClockCircleOutlined)} "Timers"]
    [:> antd/Menu.SubMenu {:title "Admin" :icon (r/create-element icons/TeamOutlined) :key "2"}
     [:> antd/Menu.Item {:key "2.1"} "Members"]
     [:> antd/Menu.Item {:key "2.2"} "Tasks"]]]])

(defn- user-and-organization-header []
  [:> antd/Layout.Header
   [:> antd/Row {:justify "space-between"}
    [header-logo]
    [:> antd/Dropdown {:trigger "click"
                       :overlay (r/create-element (r/reactify-component organization-select-menu))}
     [:> icons/DownOutlined {:style {:color "wheat" :fontSize "32px"}}]]]])

(defn with-organization-sidebar [_]
  (rf/dispatch [::org-events/fetch-organizations])
  (fn [_]
    (let [children (r/children (r/current-component))]
      [:<>
       [:> antd/Layout
        [user-and-organization-header]
        [:> antd/Layout
         [organization-sider]
         (into [:> antd/Layout.Content] children)]]])))
