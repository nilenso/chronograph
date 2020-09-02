(ns chronograph.utils.json
  (:require [cheshire.generate :as json-gen]
            [chronograph.utils.time :as tt])
  (:import [java.time Instant]))

(json-gen/add-encoder Instant
                      (fn [instant jsonGenerator]
                        (.writeString jsonGenerator (.toString instant))))
