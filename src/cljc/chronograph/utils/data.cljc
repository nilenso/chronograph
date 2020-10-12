(ns chronograph.utils.data)

(defn normalize-by
  "Like group-by, except f is expected to be unique
  for each value in the collection. Returns a map of
  keys returned by f to the values in the collection."
  [f coll]
  (zipmap (map f coll) coll))
