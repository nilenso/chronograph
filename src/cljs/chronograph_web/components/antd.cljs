(ns chronograph-web.components.antd
  "Wrapper components for the antd library"
  (:refer-clojure :exclude [list])
  (:require ["antd" :as antd]
            [medley.core :as medley]
            [reagent.core :as r]))

(defn- split-attributes
  [args]
  (if (map? (first args))
    [(first args) (rest args)]
    [{} args]))

(defn- transform-render-fn
  [render-fn]
  (fn [& args]
    ;; The render function expects a JS object. Something
    ;; (maybe antd/Table) magically turns our nice Clojure
    ;; into JS. Hence we use js->clj
    (r/as-element (apply render-fn (map #(js->clj % :keywordize-keys true) args)))))

(defn- antd-wrapper
  "Wraps an antd component given the raw React component
   and a function to transform its attributes."
  ([component]
   (antd-wrapper component identity))
  ([component attr-fn]
   (fn [& args]
     (let [[attributes children] (split-attributes args)]
       (into [:> component (attr-fn attributes)]
             children)))))

;; Layout

(def row (antd-wrapper antd/Row))
(def col (antd-wrapper antd/Col))

(def layout (antd-wrapper antd/Layout))
(def layout-header (antd-wrapper antd/Layout.Header
                                 #(assoc % :style {:background-color "white"
                                                   :border-bottom    "1px solid #d9d9d9"})))
(def layout-content (antd-wrapper antd/Layout.Content))

;; Menu

(def menu (antd-wrapper antd/Menu))

;; Call these two components like functions; don't put them in vectors
;; like Hiccup.
;; This is because the styling seems to break if an intermediary component
;; is introduced.
;; Ex: (menu-item {:key "1" :icon icons/SettingOutlined} "Settings")
(def menu-item (antd-wrapper antd/Menu.Item
                             #(medley/update-existing %
                                                      :icon
                                                      r/create-element)))
(def menu-divider (antd-wrapper antd/Menu.Divider))

;; Others

(def input (antd-wrapper antd/Input))
(def button (antd-wrapper antd/Button
                          #(medley/update-existing %
                                                   :icon
                                                   r/create-element)))

(def list (antd-wrapper antd/List
                        #(medley/update-existing %
                                                 :renderItem
                                                 transform-render-fn)))

(def list-item (antd-wrapper antd/List.Item
                             #(medley/update-existing %
                                                      :actions
                                                      (partial map r/as-element))))

(def space (antd-wrapper antd/Space
                         #(medley/update-existing %
                                                  :split
                                                  r/as-element)))

(def divider (antd-wrapper antd/Divider))

(defn- update-table-columns
  [columns]
  (map (fn [column]
         (medley/update-existing column
                                 :render
                                 transform-render-fn))
       columns))

(def table (antd-wrapper antd/Table
                         #(update % :columns update-table-columns)))

(def page-header (antd-wrapper antd/PageHeader
                               #(medley/update-existing %
                                                        :extra
                                                        r/as-element)))

(def title (antd-wrapper antd/Typography.Title))

(def dropdown (antd-wrapper antd/Dropdown
                            #(medley/update-existing %
                                                     :overlay
                                                     r/as-element)))

(def avatar (antd-wrapper antd/Avatar))
