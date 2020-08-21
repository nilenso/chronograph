(defproject chronograph "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/tools.cli "1.0.194"]
                 [com.taoensso/timbre "4.10.0"]
                 [aero "1.1.6"]
                 [mount "0.1.16"]
                 [tolitius/mount-up "0.1.3"]
                 [http-kit "2.4.0"]
                 [bidi "2.1.6"]
                 [ring "1.8.1"]
                 [cheshire "5.10.0"]
                 [com.google.api-client/google-api-client "1.30.4"]]
  :repl-options {:init-ns dev.repl-utils}
  :source-paths ["src/clj" "src/cljc"]
  :test-paths ["test/clj" "test/cljc"]
  :main ^:skip-aot chronograph.core
  :profiles {:cljs {:source-paths ["src/cljs" "src/cljc" "test/cljs"]
                    :dependencies ^:replace [[re-frame "1.0.0"]
                                             [day8.re-frame/http-fx "0.2.1"]

                                             ;; dev dependencies
                                             [org.clojure/clojure "1.10.1"]
                                             [thheller/shadow-cljs "2.10.21"]
                                             [org.clojure/core.async "1.3.610"]
                                             [day8.re-frame/re-frame-10x "0.7.0"]
                                             [binaryage/devtools "1.0.2"]]}})
