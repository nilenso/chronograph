(ns chronograph.utils.json
  (:require [cheshire.generate :as json-gen])
  (:import [java.time Instant LocalDate]))

(json-gen/add-encoder Instant
                      (fn [instant jsonGenerator]
                        (.writeString jsonGenerator (.toString instant))))

(json-gen/add-encoder LocalDate
                      (fn [local-date jsonGenerator]
                        (.writeString jsonGenerator (.toString local-date))))
