(ns chronograph-web.pages.welcome.views
  (:require [chronograph-web.page-container.views :as page-container]
            [chronograph-web.components.antd :as antd]
            [chronograph-web.pages.welcome.subscriptions :as welcome-subs]
            [re-frame.core :as rf]
            [chronograph-web.pages.welcome.events :as welcome-events]
            [chronograph-web.components.invites :as invites]
            [chronograph-web.events.organization-invites :as org-invites-events]
            [chronograph-web.routes :as routes]
            [chronograph-web.config :as config]))

(defn welcome-page [_]
  (let [invited-orgs @(rf/subscribe [::welcome-subs/invites])]
    [page-container/generic-page-container
     [antd/page-header {:title "Welcome! "}
      (if (empty? invited-orgs)
        [:p
         "You aren't part of any organizations. To get started, ask for an invite, or "
         [:a {:href (routes/path-for :new-organization)} "create your own."]]
        [:<>
         [:p
          (str "You have invites "
               (:party-popper config/emojis)
               "  !  Join one of these organizations to get started, or ")
          [:a {:href (routes/path-for :new-organization)} "create your own."]]
         [invites/invited-organizations-list
          invited-orgs
          #(rf/dispatch [::org-invites-events/accept-invite %
                         {:on-success [::welcome-events/after-invite-accepted %]}])
          #(rf/dispatch [::org-invites-events/reject-invite %])]])]]))
