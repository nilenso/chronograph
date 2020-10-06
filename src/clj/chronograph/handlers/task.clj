(ns chronograph.handlers.task
  (:require [clojure.spec.alpha :as s]
            [chronograph.domain.task :as task]
            [chronograph.db.core :as db]
            [ring.util.response :as response]
            [next.jdbc :as jdbc])
  (:refer-clojure :exclude [update]))

(defn create [{:keys [body organization user] :as _request}]
  (jdbc/with-transaction [tx db/datasource]
    (if-not (s/valid? :tasks/create-params-handler body)
      (response/bad-request {:error "Name should be present"})
      (-> (task/create tx
                       {:tasks/name (:name body)
                        :tasks/description (:description body)}
                       organization)
          (response/response)))))

(defn index [{:keys [organization user] :as _request}]
  (jdbc/with-transaction [tx db/datasource]
    (-> (task/for-organization tx organization)
        (response/response))))

(defn update [{:keys [params body organization] :as _request}]
  (jdbc/with-transaction [tx db/datasource]
    (if-let [task (task/update tx
                               {:tasks/id (some-> params
                                                  :task-id
                                                  str
                                                  Integer/parseInt)}
                               (:updates body))]
      (response/response task)
      (response/not-found "Not found"))))

(defn archive [{:keys [params] :as _request}]
  (jdbc/with-transaction [tx db/datasource]
    (if-let [task (task/archive tx
                                {:tasks/id (some-> params
                                                   :task-id
                                                   str
                                                   Integer/parseInt)})]
      (response/response task)
      (response/not-found "Not found"))))
