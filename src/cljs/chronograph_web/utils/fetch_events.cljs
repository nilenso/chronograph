(ns chronograph-web.utils.fetch-events
  "Utilities for building fetch events, which optionally accept
  a map with :on-success and :on-failure keys for success and failure
  hooks."
  (:require [medley.core :as medley]))

(defn- add-callback-event
  [fx event response]
  (cond-> fx
    event (conj [:dispatch (conj event response)])))

(defn- swap-last-2
  "Swaps the positions of the last two items in a
  sequence."
  [v]
  (vec (concat (take (- (count v) 2) v)
               (take 2 (reverse v)))))

(defn http-request-creator
  "Call this in the initial fetch event.
  It accepts a function which takes cofx and the event vector.
  The fn should return a map with the http-xhrio effect.
  The event handler defined will optionally accept a map with
  :on-success and :on-failure keys at the end of the event vector."
  [effect-creator]
  (fn [cofx event]
    (let [{:keys [on-success on-failure]} (last event)]
      {:http-xhrio (-> (effect-creator cofx event)
                       :http-xhrio
                       (medley/update-existing :on-success conj on-success)
                       (medley/update-existing :on-failure #(or on-failure %)))})))

(defn http-success-handler
  "Call this when defining the success event.
  It accepts a function which takes cofx and the event vector,
  and returns a map of fx. The response will be appended to the event
  vector, as in re-frame-http-fx."
  [event-handler]
  (fn [cofx event]
    (let [response (last event)
          on-success (last (butlast event))]
      (-> (event-handler cofx (swap-last-2 event))
          (update :fx (fnil add-callback-event []) on-success response)))))

