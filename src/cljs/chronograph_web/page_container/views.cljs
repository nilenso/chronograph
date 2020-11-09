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

(defn- limit-text [max-length text]
  (str (subs text 0 max-length) "..."))

;; This awkward API is needed because for some reason, the tooltip won't appear
;; in the dropdown unless it wraps the :a tag. ¯\_(ツ)_/¯
(defn- overflow-protected [max-length hiccup text]
  (if (> (count text) max-length)
    [antd/tooltip {:title     text
                   :placement "right"}
     (conj hiccup (limit-text max-length text))]
    (conj hiccup text)))

(defn organizations-menu [on-click]
  (let [organizations @(rf/subscribe [::pc-subs/selectable-orgs])]
    (into [antd/menu {:mode         "inline"
                      :selectedKeys []
                      :onClick      on-click
                      :class        "org-select-menu"}]
          (conj (mapv (fn [{:keys [name slug]}]
                        (antd/menu-item {:class "org-select-menu-item"}
                                        (overflow-protected 15
                                                            [:a
                                                             {:href @(rf/subscribe [::pc-subs/switch-organization-href slug])}]
                                                            name)))
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
                      :onVisibleChange #(reset! dropdown-visible? %)
                      :class           "org-select-dropdown"}
       [antd/space {:style {:margin "16px"}
                    :align "center"}
        [org-icon]
        [:div.org-name (overflow-protected 7 [:span] name)]
        [:> icons/DownOutlined]]])))

(defn- selected-sider-menu-keys []
  (let [handler (:handler @(rf/subscribe [::subs/current-page]))]
    (case handler
      :timers-list ["1"]
      :organization-show ["2"]
      [])))

(defn- page-sider [{:keys [slug] :as current-org}]
  [antd/layout-sider
   [organizations-dropdown current-org]
   [antd/menu {:mode         "inline"
               :selectedKeys (selected-sider-menu-keys)}
    (antd/menu-item {:key "1" :icon icons/ClockCircleOutlined} [:a
                                                                {:href (routes/path-for :timers-list :slug slug)}
                                                                "Timers"])
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
