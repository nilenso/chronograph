(ns chronograph-web.config)

(goog-define request-timeout 8000)

(def api-root "/api")

(def emojis {;; Adding a trailing space at the end here, because the emoji
             ;; swallows it up in some browsers.
             :frown "\uD83D\uDE41 "})
