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
                 [migratus "1.2.8"]
                 [com.fzakaria/slf4j-timbre "0.3.19"]
                 [org.postgresql/postgresql "42.2.16"]
                 [seancorfield/next.jdbc "1.1.582"]
                 [ring/ring-json "0.5.0"]
                 [cheshire "5.10.0"]
                 [com.google.api-client/google-api-client "1.30.4"]
                 [buddy/buddy-sign "3.1.0"]
                 [camel-snake-kebab "0.4.1"]
                 [medley "1.3.0"]]
  :repl-options {:init-ns dev.repl-utils}
  :source-paths ["src/clj" "src/cljc"]
  :test-paths ["test/clj" "test/cljc"]
  :main ^:skip-aot chronograph.core
  :uberjar-exclusions [#"dev.*"]
  :uberjar-name "chronograph.jar"
  :jar-name "chronograph-slim.jar"
  :plugins [[lein-cljfmt "0.7.0"]]
  :profiles {:uberjar {:aot [#"chronograph.*"]}
             :cljs {:source-paths ["src/cljs" "src/cljc" "test/cljs"]
                    :dependencies ^:replace [[re-frame "1.1.1"]
                                             [day8.re-frame/http-fx "0.2.1"]
                                             [kibu/pushy "0.3.8"]
                                             [bidi "2.1.6"]
                                             [medley "1.3.0"]

                                             ;; dev dependencies
                                             [org.clojure/clojure "1.10.1"]
                                             [thheller/shadow-cljs "2.10.21"]
                                             [org.clojure/core.async "1.3.610"]
                                             [day8.re-frame/re-frame-10x "0.7.0"]
                                             [day8.re-frame/test "0.1.5"]
                                             [binaryage/devtools "1.0.2"]]}
             :dev {:dependencies [[mock-clj "0.2.1"]
                                  [org.clojure/test.check "0.9.0"]
                                  [vvvvalvalval/scope-capture "0.3.2"]]}}
  :cljfmt {:paths   ["src" "test"]
           :indents {#"rf/reg-.*" [[:inner 0]]
                     tu/rf-test   [[:inner 0]]}})
