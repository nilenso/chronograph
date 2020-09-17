(ns chronograph.utils.json
  (:require [cheshire.generate :as json-gen])
  (:import [java.time Instant]))

(json-gen/add-encoder Instant
                      (fn [instant jsonGenerator]
                        (.writeString jsonGenerator (.toString instant))))
