(ns chronograph-web.pages.pending-invites.views
  (:require [re-frame.core :as rf]
            [chronograph-web.pages.pending-invites.events :as invites-events]
            [chronograph-web.pages.pending-invites.subscriptions :as invites-subs]))

(defn pending-invites-page [_]
  (rf/dispatch [::invites-events/page-mounted])
  (fn [_]
    (let [invites @(rf/subscribe [::invites-subs/invites])]
      [:div
       [:ul
        (map (fn [{:keys [id name]}]
               [:li
                [:p name]
                [:button {:on-click #(rf/dispatch [::invites-events/reject-invite id])}
                 "reject"]
                [:button {:on-click #(rf/dispatch [::invites-events/accept-invite id])}
                 "accept"]])
             invites)]])))
