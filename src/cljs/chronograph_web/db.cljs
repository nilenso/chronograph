(ns chronograph-web.db)

(def default-db {:user {;; signin-state can be:
                        ;; :signed-in, :signed-out or :fetching-profile
                        :signin-state :fetching-profile}})
