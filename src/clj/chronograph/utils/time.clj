(ns chronograph.utils.time
  (:import (java.time Instant)))

(defn now ^Instant []
  (Instant/now))
