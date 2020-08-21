(ns chronograph-web.views
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]
            [chronograph-web.subscriptions :as subs]
            [chronograph-web.google :as google]))

(defn- signin-button []
  (reagent/create-class
    {:display-name        "signin-button"
     :component-did-mount (fn [_]
                            (-> js/gapi
                                .-signin2
                                (.render "google-signin-button"
                                         (clj->js {:onsuccess (fn [^js google-user]
                                                                (-> google-user
                                                                    .getAuthResponse
                                                                    (google/process-auth-response)))
                                                   :onfailure (fn []
                                                                (println "Sign-in failed!"))}))))

     :reagent-render      (fn []
                            [:div {:id "google-signin-button"}])}))

(defn signin-page []
  [:a {:href "/google-login"}
   "Sign in with Google"])

(defn root []
  [signin-page])
