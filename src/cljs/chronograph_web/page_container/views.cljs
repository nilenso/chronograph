(ns chronograph-web.page-container.views
  (:require [re-frame.core :as rf]
            [chronograph-web.events.organization :as org-events]
            [chronograph-web.page-container.events :as pc-events]
            [chronograph-web.subscriptions :as subs]
            [chronograph-web.page-container.subscriptions :as pc-subs]
            [chronograph-web.components.antd :as antd]
            ["@ant-design/icons" :as icons]
            [chronograph-web.routes :as routes]
            [reagent.core :as r]))

(defn- user-menu []
  [antd/menu {:mode "inline"}
   (antd/menu-item {:key "1" :icon icons/SettingOutlined} "Settings")
   (antd/menu-divider {:key "2"})
   (antd/menu-item {:key "3" :icon icons/LogoutOutlined} "Logout")])

(defn- user-dropdown [name photo-url]
  [antd/dropdown {:overlay [user-menu]}
   [antd/space
    [antd/avatar {:size "large"
                  :src  photo-url}]
    name
    [:> icons/DownOutlined]]])

(defn- page-header [{:keys [name photo-url]}]
  [antd/layout-header
   [antd/row {:justify "end"
              :align   "middle"}
    [user-dropdown name photo-url]]])

(defn organizations-menu [on-click]
  (let [organizations @(rf/subscribe [::pc-subs/selectable-orgs])]
    (into [antd/menu {:mode         "inline"
                      :selectedKeys []
                      :onClick      on-click}]
          (conj (mapv (fn [{:keys [name slug]}]
                        (antd/menu-item [:a
                                         {:href @(rf/subscribe [::pc-subs/switch-organization-href slug])}
                                         name]))
                      organizations)
                (when-not (= :new-organization
                             (:handler @(rf/subscribe [::subs/current-page])))
                  (antd/menu-item [antd/button
                                   {:type    "primary"
                                    :icon    icons/PlusOutlined
                                    :onClick #(rf/dispatch [::pc-events/add-org-button-clicked])}
                                   "Add New"]))))))

(defn- org-icon []
  [:div
   [:> icons/BankOutlined {:style {:fontSize "32px"}}]])

(defn organizations-dropdown [_]
  (let [dropdown-visible? (r/atom false)]
    (fn [{:keys [name]}]
      [antd/dropdown {:overlay         [organizations-menu #(reset! dropdown-visible? false)]
                      :trigger         "click"
                      :visible         @dropdown-visible?
                      :onVisibleChange #(reset! dropdown-visible? %)}
       [antd/space {:style {:margin "16px"}
                    :align "center"}
        [org-icon]
        [:span {:style {:fontSize "32px"}} name]
        [:> icons/DownOutlined]]])))

(defn- selected-sider-menu-keys []
  (let [handler (:handler @(rf/subscribe [::subs/current-page]))]
    (case handler
      :overview ["1"]
      :organization-show ["2"]
      [])))

(defn- page-sider [{:keys [slug] :as current-org}]
  [antd/layout-sider
   [organizations-dropdown current-org]
   [antd/menu {:mode         "inline"
               :selectedKeys (selected-sider-menu-keys)}
    (antd/menu-item {:key "1" :icon icons/ClockCircleOutlined} [:a
                                                                {:href (routes/path-for :overview :slug slug)}
                                                                "Overview"])
    (antd/menu-item {:key "2" :icon icons/TeamOutlined} [:a
                                                         {:href (routes/path-for :organization-show :slug slug)}
                                                         "Admin"])]])

(defn org-scoped-page-container
  "Used when the page is specific to an organization."
  [& _]
  (rf/dispatch [::org-events/fetch-organizations])
  (fn [& children]
    (let [user-info   @(rf/subscribe [::subs/user-info])
          current-org @(rf/subscribe [::pc-subs/current-org])]
      [antd/layout
       (when current-org
         [page-sider current-org])
       [antd/layout
        (when user-info
          [page-header user-info])
        (into [antd/layout-content {:style {:min-height "300px"}}] children)]])))

(defn generic-page-container
  "Used when the page is not specific to an organization."
  [& children]
  (let [user-info @(rf/subscribe [::subs/user-info])]
    [antd/layout
     [antd/layout-sider]
     [antd/layout
      (when user-info
        [page-header user-info])
      (into [antd/layout-content {:style {:min-height "300px"}}] children)]]))
