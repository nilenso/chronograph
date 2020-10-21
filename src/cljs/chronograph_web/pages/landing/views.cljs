(ns chronograph-web.pages.landing.views
  (:require [re-frame.core :as rf]
            [chronograph-web.events.organization :as org-events]
            [chronograph-web.subscriptions :as subs]
            [chronograph-web.pages :as pages]
            [chronograph-web.pages.landing.events :as landing-events]
            [chronograph-web.pages.landing.subscriptions :as landing-subs]
            [chronograph-web.components.antd :as antd]
            ["@ant-design/icons" :as icons]))

(defn- organizations-table [organizations]
  (when (not-empty organizations)
    [:<>
     [antd/title {:level 4} "My Organizations"]
     [antd/table {:rowKey     "id"
                  :columns    [{:title     "Name"
                                :dataIndex "name"
                                :key       "name"
                                :render    (fn [org-name {:keys [slug] :as _org}]
                                             [:a
                                              {:href (str "/organizations/" slug)}
                                              org-name])}
                               {:title     "Role"
                                :dataIndex "role"
                                :key       "role"}]
                  :dataSource (->> organizations
                                   vals
                                   (map #(assoc % :role "Unknown")))}]]))

(defn- invited-organizations-list
  [organizations]
  (when (not-empty organizations)
    [:<>
     [antd/title {:level 4} "My Invitations"]
     [antd/list {:renderItem (fn [{:keys [name id] :as _org}]
                               [antd/list-item {}
                                [antd/space {:split [antd/divider {:type "vertical"}]}
                                 name
                                 [antd/button {:onClick #(rf/dispatch [::landing-events/accept-invite id])
                                               :type    "link"}
                                  "Accept"]
                                 [antd/button {:onClick #(rf/dispatch [::landing-events/reject-invite id])
                                               :danger  true
                                               :type    "link"}
                                  "Decline"]]])
                 :dataSource organizations}]]))

(defn landing-page [_]
  (rf/dispatch [::org-events/fetch-organizations])
  (rf/dispatch [::landing-events/fetch-invited-orgs])
  (fn [_]
    (let [organizations         @(rf/subscribe [::subs/organizations])
          invited-organizations @(rf/subscribe [::landing-subs/invites])]
      [pages/with-user-header
       [antd/page-header {:extra      [antd/button
                                       {:type "primary"
                                        :icon icons/PlusOutlined
                                        :href "/organizations/new"}
                                       "Create"]
                          :breadcrumb {:routes [{:path           ""
                                                 :breadcrumbName "Home"}]}}
        [invited-organizations-list invited-organizations]
        (when (not-empty invited-organizations)
          [antd/divider])
        [organizations-table organizations]]])))
