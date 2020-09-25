(ns chronograph.utils.coercions)

(defn str-to-uuid
  [s]
  (when (string? s)
    (try (java.util.UUID/fromString s)
         (catch IllegalArgumentException _))))
