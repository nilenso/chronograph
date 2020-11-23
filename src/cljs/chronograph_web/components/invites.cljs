(ns chronograph-web.components.invites
  (:require [chronograph-web.components.antd :as antd]))

(defn invited-organizations-list
  [invited-orgs on-accept-click on-reject-click]
  (when (not-empty invited-orgs)
    [:<>
     [antd/title {:level 4} "Invitations"]
     [antd/list {:renderItem (fn [{:keys [name slug] :as _org}]
                               [antd/list-item {}
                                [antd/row {:align "middle"}
                                 [antd/col {:flex 3} name]
                                 [antd/col {:flex 2}
                                  [antd/button {:onClick #(on-accept-click slug)
                                                :type    "link"}
                                   "Accept"]
                                  [antd/button {:onClick #(on-reject-click slug)
                                                :danger  true
                                                :type    "link"}
                                   "Decline"]]]])
                 :dataSource invited-orgs}]]))
