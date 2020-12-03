(ns chronograph.domain.timer.time-span
  (:require [chronograph.utils.time :as time])
  (:import (java.time Instant)
           (java.time.temporal ChronoUnit)))

(defn- expand-stopped-at
  [{:keys [^Instant stopped-at] :as time-span} duration-in-secs stopped-at-limit]
  (if (nil? stopped-at)
    [duration-in-secs time-span]
    (let [expand-limit (when stopped-at-limit
                         (.between ChronoUnit/SECONDS stopped-at stopped-at-limit))]
      (if (and expand-limit (< expand-limit duration-in-secs))
        [(- duration-in-secs expand-limit) (assoc time-span
                                                  :stopped-at
                                                  (.plusSeconds stopped-at expand-limit))]
        [0 (assoc time-span :stopped-at (.plusSeconds stopped-at duration-in-secs))]))))

(defn- expand-started-at
  [{:keys [^Instant started-at] :as time-span} duration-in-secs started-at-limit]
  (let [expand-limit (when started-at-limit
                       (.between ChronoUnit/SECONDS started-at-limit started-at))]
    (if (and expand-limit (< expand-limit duration-in-secs))
      [(- duration-in-secs expand-limit) (assoc time-span
                                                :started-at
                                                (.minusSeconds started-at expand-limit))]
      [0 (assoc time-span
                :started-at
                (.minusSeconds started-at duration-in-secs))])))

(defn- expand-time-span
  "Tries to expand the time span to the given duration,
  keeping in mind the (optional) limits for started-at and stopped-at.

  Attempts to change the stop time before the start time."
  [time-span duration-in-secs started-at-limit stopped-at-limit]
  (if (= 0 duration-in-secs)
    [duration-in-secs time-span]
    (let [[remaining-duration expanded-time-span-1] (expand-stopped-at time-span duration-in-secs stopped-at-limit)]
      (expand-started-at expanded-time-span-1 remaining-duration started-at-limit))))

(defn- replace-last-element
  [a-vector an-item]
  (conj (vec (butlast a-vector)) an-item))

(defn- add-duration-to-time-spans*
  [time-spans duration-in-secs stopped-at-limit]
  (if (empty? time-spans)
    (if (> duration-in-secs 0)
      [{:started-at (.minusSeconds stopped-at-limit duration-in-secs)
        :stopped-at stopped-at-limit}]
      [])
    (let [started-at-limit (-> time-spans
                               butlast
                               last
                               :stopped-at)
          ;; Try to add the duration to the last time span,
          ;; to the extent possible.
          [remaining-duration expanded-span] (expand-time-span (last time-spans)
                                                               duration-in-secs
                                                               started-at-limit
                                                               stopped-at-limit)]
      (if (= remaining-duration 0)
        (replace-last-element time-spans expanded-span)
        ;; If there's still some seconds left to add, try to
        ;; add it to the previous time spans.
        (conj (add-duration-to-time-spans* (vec (butlast time-spans))
                                           remaining-duration
                                           (:started-at expanded-span))
              expanded-span)))))

(defn- merge-time-spans
  "If one time span starts when the previous time span ends,
  they are merged together."
  [time-spans]
  (reduce
   (fn [new-time-spans time-span]
     (if (= (:started-at time-span)
            (-> new-time-spans
                last
                :stopped-at))
       (replace-last-element new-time-spans
                             {:started-at (:started-at (last new-time-spans))
                              :stopped-at (:stopped-at time-span)})
       (conj new-time-spans time-span)))
   []
   time-spans))

(defn- add-duration-to-time-spans
  "Adds the given duration to the time spans.
  stopped-at-limit should be the current time"
  [time-spans duration-in-secs stopped-at-limit]
  (merge-time-spans (add-duration-to-time-spans* time-spans duration-in-secs stopped-at-limit)))

(defn- time-span-duration
  [{:keys [started-at stopped-at]} current-time]
  (.between ChronoUnit/SECONDS started-at (or stopped-at current-time)))

(defn total-duration-in-seconds
  [time-spans current-time]
  (->> time-spans
       (map #(time-span-duration % current-time))
       (reduce +)))

(defn running?
  [time-spans]
  (and (not-empty time-spans)
       (not (:stopped-at (last time-spans)))))

(defn- subtract-duration-from-time-spans
  "Removes the given duration from the time spans.
  Assumes that the duration is greater than or equal to
  the total duration of the time spans."
  [time-spans duration-in-secs current-time]
  (if (empty? time-spans)
    []
    (let [time-span-to-adjust     (last time-spans)
          last-time-span-duration (time-span-duration time-span-to-adjust current-time)
          time-spans-but-last     (vec (butlast time-spans))
          time-span-before-last (last time-spans-but-last)]
      (cond
        (= 0 duration-in-secs)
        time-spans

        (< last-time-span-duration duration-in-secs)
        ;; This means we cannot subtract the entire duration from
        ;; the last time span. So remove it, and try to work with
        ;; the previous time spans.
        (if (running? time-spans)
          (recur (replace-last-element time-spans-but-last
                                       (assoc time-span-before-last :stopped-at nil))
                 ;; Removing the last running time span will increase the total
                 ;; duration of the time spans, since the stopped-at of the second
                 ;; to last time span will be set to nil. So we compensate for that
                 ;; by increasing the duration to be removed.
                 (+ duration-in-secs
                    (.between ChronoUnit/SECONDS
                              (:stopped-at time-span-before-last)
                              (:started-at time-span-to-adjust)))
                 current-time)
          (recur time-spans-but-last
                 (- duration-in-secs last-time-span-duration)
                 current-time))

        (= last-time-span-duration duration-in-secs)
        (if (running? time-spans)
          ;; We need to remove the last element, but the timer
          ;; needs to be running. So we replace it with this,
          ;; which is effectively an empty time span.
          (replace-last-element time-spans {:started-at current-time
                                            :stopped-at nil})
          time-spans-but-last)

        (> last-time-span-duration duration-in-secs)
        ;; We can subtract the entire duration from the last
        ;; time span. So we just move the started-at forward.
        (replace-last-element time-spans
                              (update time-span-to-adjust
                                      :started-at
                                      #(.plusSeconds ^Instant %
                                                     duration-in-secs)))))))

(defn adjust-time-spans-to-duration
  "Adjusts the time spans so that they add up to the given duration.

  This function avoids heavy-handed editing and tries to preserve as much
  of the time span data as possible. It attempts to edit time spans in reverse
  chronological order. The assumption is that the user wants to correct errors
  in the more recent time spans rather than the oldest ones.

  If the duration of the time spans needs to be reduced, only the start time
  of the time spans will be changed. If the duration needs to be increased, the
  stop time is changed followed by the start time. "
  ([time-spans duration-in-secs]
   (adjust-time-spans-to-duration time-spans duration-in-secs (time/now)))
  ([time-spans duration-in-secs current-time]
   (let [total-duration      (total-duration-in-seconds time-spans current-time)
         duration-difference (- duration-in-secs total-duration)]
     (cond
       (> duration-difference 0) (add-duration-to-time-spans time-spans
                                                             (- duration-in-secs total-duration)
                                                             current-time)
       (= duration-difference 0) time-spans
       (< duration-difference 0) (subtract-duration-from-time-spans time-spans
                                                                    (- total-duration duration-in-secs)
                                                                    current-time)))))
