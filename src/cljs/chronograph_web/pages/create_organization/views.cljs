(ns chronograph-web.pages.create-organization.views
  (:require [chronograph-web.components.form :as form]
            [chronograph-web.http :as http]
            [chronograph-web.components.antd :as antd]
            [chronograph-web.routes :as routes]
            [chronograph-web.pages.create-organization.events :as create-org-events]
            [chronograph-web.page-container.views :as page-container]
            [clojure.string :as string]))

(defn- timers-url-prefix []
  (let [slug "default"]
    (-> (str js/location.origin
             (routes/path-for :timers-list :slug slug))
        (string/split (re-pattern slug))
        (first))))

(defn- timers-url-suffix []
  (let [slug "default"]
    (-> (routes/path-for :timers-list :slug slug)
        (string/split (re-pattern slug))
        (second))))

(defn- create-organization-form []
  (let [{::form/keys [get-input-attributes get-submit-attributes]}
        (form/form {:form-key        ::create-organization
                    :request-builder (fn [{name :name slug :slug}]
                                       (http/post {:uri        "/api/organizations/"
                                                   :params     {:name name
                                                                :slug slug}
                                                   :on-success [::create-org-events/create-organization-succeeded]
                                                   :on-failure [::create-org-events/create-organization-failed]}))})]
    (fn []
      [:form {:style {:padding-bottom "8px"}}
       [antd/space {:direction "vertical"}
        [antd/input (get-input-attributes :name {:type :text :autoFocus true} :organizations/name)]
        [antd/input (get-input-attributes :slug
                                          {:placeholder "e.g. my-org-name-42"
                                           :addonBefore (timers-url-prefix)
                                           :addonAfter  (timers-url-suffix)}
                                          :organizations/slug)]
        [antd/space
         [antd/button (get-submit-attributes) "Save"]]]])))

(defn create-organization-page [_]
  [page-container/generic-page-container
   [antd/page-header {:onBack #(js/history.back)
                      :title "Create New Organization"}
    [create-organization-form]]])

