(ns chronograph-web.pages.landing.views
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [chronograph-web.events.organization :as org-events]
            [chronograph-web.subscriptions :as subs]
            [chronograph-web.pages :as pages]
            ["antd" :as antd]
            ["@ant-design/icons" :as icons]))

;; TODO: This needs to go
(set! *warn-on-infer* false)

(defn- organizations-table [organizations]
  [:> antd/Table {:rowKey "id"
                  :columns [{:title "Name"
                             :dataIndex "name"
                             :key "name"
                             :render #(r/create-element
                                       (r/reactify-component
                                        (fn [] [:a
                                                {:href (str "/organizations/"
                                                            (. %2 -slug))}
                                                %1])))}
                            {:title "Role"
                             :dataIndex "role"
                             :key "role"}]
                  :dataSource (->> organizations
                                   vals
                                   (map #(assoc % :role "Unknown")))}])

(defn organizations-list [organizations]
  [:div
   (when (not-empty organizations)
     (organizations-table organizations))])

(defn landing-page [_]
  (fn []
    (rf/dispatch [::org-events/fetch-organizations])
    (let [organizations @(rf/subscribe [::subs/organizations])]
      [pages/with-user-header
       [:> antd/PageHeader {:title "Organizations"
                            :extra (r/create-element (r/reactify-component (fn []
                                                                             [:>
                                                                              antd/Button
                                                                              {:type "primary"
                                                                               :icon (r/create-element icons/PlusOutlined)
                                                                               :href "/organizations/new"}
                                                                              "Create"])))
                            :breadcrumb {:routes [{:path "organizations"
                                                   :breadcrumbName "Organizations"}]}}
        [organizations-list organizations]]])))

