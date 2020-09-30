(ns chronograph-web.pages.pending-invites.views
  (:require [re-frame.core :as rf]
            [chronograph-web.pages.pending-invites.events :as invites-events]
            [chronograph-web.pages.pending-invites.subscriptions :as invites-subs]))

(defn pending-invites-page [_]
  (let [invites @(rf/subscribe [::invites-subs/invites])]
    [:div
     [:ul
      (map #(let [id (:id %)
                  org-name (:name %)]
              (println org-name)
              [:li
               [:p org-name]
               [:button {:on-click (fn []
                                     (rf/dispatch [::invites-events/reject-invite id]))}
                "reject"]
               [:button {:on-click (fn []
                                     (rf/dispatch [::invites-events/accept-invite id]))}
                "accept"]])
           @(rf/subscribe [::invites-subs/invites]))]]))