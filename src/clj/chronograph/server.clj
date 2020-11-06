(ns chronograph.server
  (:require [mount.core :refer [defstate]]
            [bidi.ring :as bidi]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [taoensso.timbre :as log]
            [org.httpkit.server :as httpkit]
            [ring.util.response :as response]
            [chronograph.config :as config]
            [chronograph.domain.acl :as acl]
            [chronograph.middleware :as middleware]
            [chronograph.handlers.google-auth :as google-auth]
            [chronograph.handlers.task :as task]
            [chronograph.handlers.timer :as timer]
            [chronograph.handlers.user :as user]
            [chronograph.handlers.organization :as organization]
            [chronograph.handlers.invite :as invite]))

(def google-auth-routes
  [["login" google-auth/login-handler]
   ["web/redirect" google-auth/web-redirect-handler]
   ["desktop/redirect" google-auth/desktop-redirect-handler]])

(def task-routes
  {:post                 (-> task/create
                             (middleware/wrap-require-authorization #{acl/admin}))
   :get                  (-> task/index
                             (middleware/wrap-require-authorization #{acl/admin acl/member}))
   [:task-id]            {:put (-> task/update
                                   (middleware/wrap-require-authorization #{acl/admin}))}
   [:task-id "/archive"] {:put (-> task/archive
                                   (middleware/wrap-require-authorization #{acl/admin}))}
   [:task-id "/timers"]  {:get (-> timer/find-by-user-and-task
                                   middleware/wrap-authenticated)}})

(def timer-routes
  {:post (-> timer/create
             middleware/wrap-authenticated)
   :get (-> timer/find-by-day
            middleware/wrap-authenticated)
   [:timer-id] {:delete (-> timer/delete
                            middleware/wrap-authenticated)}
   [:timer-id "/start"] {:put (-> timer/start
                                  middleware/wrap-authenticated)}
   [:timer-id "/stop"] {:put (-> timer/stop
                                 middleware/wrap-authenticated)}
   [:timer-id "/update-note"] {:put (-> timer/update-note
                                        middleware/wrap-authenticated)}})

(def invited-org-routes
  {:get                  (-> invite/invited-orgs
                             middleware/wrap-authenticated)
   ["/" :slug "/accept"] {:post (-> invite/accept
                                    middleware/wrap-authenticated)}
   ["/" :slug "/reject"] {:post (-> invite/reject
                                    middleware/wrap-authenticated)}})

(def organizations-routes {:get              (-> organization/index
                                                 middleware/wrap-authenticated)
                           :post             (-> organization/create
                                                 middleware/wrap-authenticated)
                           ["" :slug]        {:get       (-> organization/find-one
                                                             middleware/wrap-authenticated)
                                              "/members" {:get  (-> organization/show-members
                                                                    middleware/wrap-authenticated)
                                                          :post (-> organization/invite
                                                                    middleware/wrap-authenticated)}}
                           [:slug "/tasks/"] task-routes})

(def routes
  ["/" [["" (fn [_] (-> (response/resource-response "public/index.html")
                        (response/content-type "text/html")))]
        ["auth/" [["google/" google-auth-routes]]]
        ["api/" [["users/me" {:get (-> user/me
                                       middleware/wrap-authenticated)}]
                 ["timers" timer-routes]
                 ["invitations" invited-org-routes]
                 ["organizations/" organizations-routes]]]
        [true (fn [_] (-> (response/resource-response "public/index.html")
                          (response/content-type "text/html")))]]])

(def handler
  (-> routes
      bidi/make-handler
      (middleware/wrap-exception-logging)
      (wrap-json-response {:key-fn name})
      (wrap-json-body {:keywords? true :bigdecimals? true})
      (wrap-keyword-params)
      (wrap-params)
      (wrap-cookies)
      (wrap-resource "public")
      (wrap-content-type)
      (wrap-not-modified)))

(defn- error-logger [text ex]
  (log/error ex text))

(defn- warn-logger [text ex]
  (log/warn ex text))

(defn start-server!
  ([] (start-server! handler))
  ([app-handler]
   (log/info {:event ::server-start})
   (httpkit/run-server app-handler
                       {:port         (:port config/config)
                        :error-logger error-logger
                        :warn-logger  warn-logger})))

(defstate server
  :start (start-server!)
  :stop (server))
