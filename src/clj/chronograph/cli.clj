(ns chronograph.cli
  (:require [clojure.set :as set]
            [clojure.tools.cli :as tcli]
            [clojure.string :as str]))

;; Our cli takes some options and one optional argument which denotes the mode of operation

(def ^:private cli-options
  [["-f" "--config-file FILE" "Path to configuration file"]
   ["-h" "--help" "Print this help message"]
   ["-r" "--rollback" "Rollback the last migration. Must be run alone"]
   ["-m" "--migrate" "Run migrations"]
   ["-s" "--serve" "Run the web server"]])

(defn- operational-modes [options]
  (set/intersection #{:help :migrate :rollback :serve} (into #{} (keys options))))

(defn parse [args]
  (tcli/parse-opts args cli-options))

(defn error-message [{:keys [options]}]
  (cond
    (not= 1 (count (operational-modes options)))
    "Should be invoked with exactly one of -h -r -m -s"

    (not (or (:config-file options) (:help options)))
    "Missing required option -f"

    :else nil))

(defn operational-mode [{:keys [options]}]
  (first (operational-modes options)))

(defn help-message [{:keys [summary]}]
  (str
   (str/join "\n\n" ["The chronograph server"
                     "Use only one option of -s or -h at once"
                     summary])
   "\n"))

