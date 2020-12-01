(ns chronograph.utils.string)

(defn left-pad [n val s]
  (if (>= (count s) n)
    s
    (->> s
         (concat (repeat (- n (count s)) val))
         (apply str))))
