(ns chronograph-web.interceptors
  (:require [cljs.spec.alpha :as s]
            [re-frame.core :as rf]
            [chronograph-web.db.spec :as db-spec]))

(defn check-and-print
  "Prints an error if the db doesn't match the spec."
  [a-spec db]
  (when-not (s/valid? a-spec db)
    (.error js/console (str "Spec check failed!\n" (s/explain-str a-spec db)))))

(defn reg-interceptor-after
  "Accepts an id and a function which accepts the DB and
  performs some side effect, and registers it as an interceptor."
  [id f]
  (rf/->interceptor
   :id id
   :after (fn [context]
            (let [db (or (rf/get-effect context :db)
                         (rf/get-coeffect context :db))]
              (f db)
              context))))

(def check-spec-and-print-interceptor (reg-interceptor-after ::check-spec-and-print
                                                             (partial check-and-print ::db-spec/db)))
