(ns chronograph-web.pages.pending-invites.views
  (:require [re-frame.core :as rf]
            [chronograph-web.pages.pending-invites.events :as invites]))

(defn pending-invites-page [_]
  [:div 
   [:p "invite1"]
   [:button {:on-click (fn [] (rf/dispatch [::invites/reject-invite 0]))} "reject" ]
   [:button "accept"]
   [:p "invite2"]
   [:p "invite3"]])
 
 
