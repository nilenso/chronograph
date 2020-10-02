(ns chronograph-web.pages.organization.views
  (:require [re-frame.core :as rf]
            [goog.string :as gstring]
            [clojure.spec.alpha :as s]
            [chronograph-web.components.common :as components]
            [chronograph-web.components.form :as form]
            [chronograph-web.subscriptions :as subs]
            [chronograph-web.pages.organization.events :as org-events]))

(defn- form-valid? [{:keys [name description]}]
  (and
   (s/valid? :tasks/name name)
   (s/valid? :tasks/description description)))

(defn- tasks-uri [slug]
  (gstring/format "/api/organizations/%s/tasks/" slug))

(defn task-form [{:keys [slug] :as _organization}]
  (let [{::form/keys [input submit]}
        (form/form {:form-key  ::create-task
                    :form-spec :tasks/task
                    :method :post
                    :uri (tasks-uri slug)
                    :on-success [[::org-events/get-tasks]]})]
    [:form
     [input :name :tasks/name]
     [input :description :tasks/description]
     [submit nil "Save"]]))

(defn update-form [{:keys [id] :as _task}]
  (let [{:keys [form-params status]} @(rf/subscribe [::subs/update-task-form id])
        form-invalid? (not (form-valid? form-params))
        {:keys [name description]} form-params]
    [:form
     [components/text-input :name :tasks/name
      {:placeholder "Name"
       :value name
       :on-change #(rf/dispatch [::org-events/update-task-form-update
                                 id
                                 :name
                                 (.-value (.-currentTarget %))])}]
     [components/text-input :description :tasks/description
      {:placeholder "Description"
       :value description
       :on-change #(rf/dispatch [::org-events/update-task-form-update
                                 id
                                 :description
                                 (.-value (.-currentTarget %))])}]
     [:button {:type :button
               :name :save
               :disabled (or (= status :saving)
                             form-invalid?)
               :on-click (fn [] (rf/dispatch [::org-events/update-task-form-submit id]))}
      (if (= status :saving)
        "Saving..."
        "Save")]
     [:button {:type :button
               :name :save
               :disabled (or (= status :saving)
                             form-invalid?)
               :on-click (fn [] (rf/dispatch [::org-events/cancel-update-task-form id]))}
      "Cancel"]]))

(defn task-list-element [{:keys [id name description is-updating] :as task}]
  [:li {:key id}
   [:div
    (if (true? is-updating)
      [update-form task]
      [:p [:b name] " - " description])
    [:button {:on-click #(rf/dispatch [::org-events/archive-task id])}
     "Archive"]
    [:button {:on-click #(rf/dispatch [::org-events/show-update-form task])}
     "Update"]]])

(defn task-list [tasks]
  [:div
   [:h3 "Tasks"]
   [:ul
    (not-empty (map task-list-element tasks))]])

(defn organization-page [{:keys [slug]}]
  (rf/dispatch [::org-events/fetch-organization slug])
  (rf/dispatch [::org-events/fetch-tasks slug])
  (fn [_]
    (when-let [{:keys [id name slug created-at updated-at]
                :as organization}
               @(rf/subscribe [::subs/organization slug])]
      (if (= organization ::org-events/not-found)
        [:p "Not found"]

        [:div
         [:strong "Organization details: "]
         [:ul
          [:li "ID: " id]
          [:li "Name: " name]
          [:li "Slug: " slug]
          [:li "Created at: " created-at]
          [:li "Updated at: " updated-at]]
         [task-form organization]
         (when-let [tasks @(rf/subscribe [::subs/tasks])]
           [task-list tasks])]))))
