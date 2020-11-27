(ns chronograph-web.components.common
  (:require [chronograph-web.components.antd :as antd]))

(defn full-page-spinner []
  [antd/layout
   [antd/layout-content
    [antd/row {:justify "center"
               :style   {:margin-top "200px"}}
     [antd/spin {:size "large"}]]]])
