(ns chronograph-web.components.antd
  "Wrapper components for the antd library"
  (:refer-clojure :exclude [list])
  (:require ["antd" :as antd]
            [medley.core :as medley]
            [reagent.core :as r]))

(defn button
  [attributes text]
  [:> antd/Button
   (medley/update-existing attributes
                           :icon
                           r/create-element)
   text])

(defn- transform-render-fn
  [render-fn]
  (fn [& args]
    ;; The render function expects a JS object. Something
    ;; (maybe antd/Table) magically turns our nice Clojure
    ;; into JS. Hence we use js->clj
    (r/as-element (apply render-fn (map #(js->clj % :keywordize-keys true) args)))))

(defn list
  [attributes]
  [:> antd/List (medley/update-existing attributes
                                        :renderItem
                                        transform-render-fn)])

(defn list-item
  [& children]
  (into [:> antd/List.Item] children))

(defn space
  [& children]
  (into [:> antd/Space] children))

(defn- update-table-columns
  [columns]
  (map (fn [column]
         (medley/update-existing column
                                 :render
                                 transform-render-fn))
       columns))

(defn table
  [attributes]
  [:> antd/Table (update attributes :columns update-table-columns)])

(defn page-header
  [attributes & children]
  (into [:> antd/PageHeader (medley/update-existing attributes
                                                    :extra
                                                    r/as-element)]
        children))

(defn title
  [& children]
  (into [:> antd/Typography.Title] children))
