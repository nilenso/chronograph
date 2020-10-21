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

(def row (antd-wrapper antd/Row))
(def col (antd-wrapper antd/Col))

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
