(ns chronograph-web.pages.landing.views
  (:require [re-frame.core :as rf]
            [chronograph-web.events.organization :as org-events]
            [chronograph-web.components.form :as form]
            [chronograph-web.http :as http]
            [chronograph-web.subscriptions :as subs]
            [chronograph-web.pages :as pages]
            [chronograph-web.pages.landing.events :as landing-events]
            [chronograph-web.pages.landing.subscriptions :as landing-subs]
            [chronograph-web.components.antd :as antd]
            ["@ant-design/icons" :as icons]
            [chronograph-web.routes :as routes]))

(defn- organizations-root-url []
  (str js/location.origin
       (routes/path-for :organization-show :slug "")))

(defn create-organization-form
  []
  (let [{::form/keys [get-input-attributes get-submit-attributes submitting?-state]}
        (form/form {:form-key ::create-organization
                    :request-builder (fn [{name :name slug :slug}]
                                       (http/post {:uri "/api/organizations/"
                                                   :params {:name name
                                                            :slug slug}
                                                   :on-success [::landing-events/create-organization-succeeded]
                                                   :on-failure [::landing-events/create-organization-failed]}))})
        page-error (rf/subscribe [::subs/page-errors])]
    (fn []
      [:form {:style {:padding-bottom "8px"}}
       (when (contains? @page-error ::landing-events/error-create-organization-failed)
         [:div "Error creating the organization"])
       [antd/space {:direction "vertical"}
        [antd/input (get-input-attributes :name {:type :text :autoFocus true} :organizations/name)]
        [antd/input (get-input-attributes :slug
                                          {:addonBefore (organizations-root-url)
                                           :placeholder "e.g. my-org-name-42"}
                                          :organizations/slug)]
        [antd/space
         [antd/button (get-submit-attributes) "Save"]
         [antd/button
          {:onClick #(rf/dispatch [::landing-events/hide-create-org-form])
           :disabled @submitting?-state}
          "Cancel"]]]])))

(defn- organizations-table [organizations]
  (let [show-create-org-form? @(rf/subscribe [::landing-subs/show-create-org-form?])]
    (when (not-empty organizations)
      [:<>
       [antd/title {:level 4} [antd/space "My Organizations"
                               (when-not show-create-org-form?
                                 [antd/button
                                  {:type "primary"
                                   :icon icons/PlusOutlined
                                   :onClick #(rf/dispatch [::landing-events/show-create-org-form])}
                                  "Add New"])]]
       (when show-create-org-form?
         [create-organization-form])
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
                                     (map #(assoc % :role "Unknown")))}]])))

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
                                  [antd/button {:onClick #(rf/dispatch [::landing-events/accept-invite id])
                                                :type    "link"}
                                   "Accept"]
                                  [antd/button {:onClick #(rf/dispatch [::landing-events/reject-invite id])
                                                :danger  true
                                                :type    "link"}
                                   "Decline"]]]])
                 :dataSource organizations}]]))

(defn landing-page [_]
  (rf/dispatch [::org-events/fetch-organizations])
  (rf/dispatch [::landing-events/fetch-invited-orgs])
  (fn [_]
    (let [organizations         @(rf/subscribe [::subs/organizations])
          invited-organizations @(rf/subscribe [::landing-subs/invites])]
      [pages/with-user-header
       [antd/page-header {:breadcrumb {:routes [{:path           ""
                                                 :breadcrumbName "Home"}]}}
        [invited-organizations-list invited-organizations]
        (when (not-empty invited-organizations)
          [antd/divider])
        [organizations-table organizations]]])))
