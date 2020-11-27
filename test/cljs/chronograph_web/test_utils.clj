(ns chronograph-web.test-utils)

(defmacro rf-test [docstring & body]
  `(cljs.test/testing ~docstring
     (day8.re-frame.test/run-test-sync
      (chronograph-web.test-utils/initialize-db!)
      (chronograph-web.test-utils/stub-routing)
      ~@body)))
