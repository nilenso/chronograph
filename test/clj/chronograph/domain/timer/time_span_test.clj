(ns chronograph.domain.timer.time-span-test
  (:require [clojure.test :refer :all]
            [chronograph.utils.time :as time]
            [chronograph.domain.timer.time-span :as time-span]))

(deftest adjust-time-spans-to-duration-test
  (testing "Adjusting a stopped set of time spans"
    (let [initial-time      (.minusSeconds (time/now) (* 3 24 60 60))
          time-spans        [{:started-at initial-time
                              :stopped-at (.plusSeconds initial-time 1800)}
                             {:started-at (.plusSeconds initial-time 3600)
                              :stopped-at (.plusSeconds initial-time 5400)}]
          fake-current-time (.plusSeconds initial-time 5600)]

      (with-redefs [time/now (constantly fake-current-time)]
        ;; TODO: Assert on the duration using a generative test.
        (is (= 1200
               (time-span/total-duration-in-seconds (time-span/adjust-time-spans-to-duration time-spans
                                                                                             1200
                                                                                             fake-current-time)
                                                    fake-current-time)))
        (is (= 1800
               (time-span/total-duration-in-seconds (time-span/adjust-time-spans-to-duration time-spans
                                                                                             1800
                                                                                             fake-current-time)
                                                    fake-current-time)))
        (is (= 4542
               (time-span/total-duration-in-seconds (time-span/adjust-time-spans-to-duration time-spans
                                                                                             4542
                                                                                             fake-current-time)
                                                    fake-current-time)))

        (is (= [{:started-at initial-time
                 :stopped-at (.plusSeconds initial-time 1800)}]
               (time-span/adjust-time-spans-to-duration time-spans
                                                        1800
                                                        fake-current-time)))

        (is (= [{:started-at (.plusSeconds initial-time 50)
                 :stopped-at (.plusSeconds initial-time 1800)}]
               (time-span/adjust-time-spans-to-duration time-spans
                                                        1750
                                                        fake-current-time)))

        (is (= [{:started-at initial-time
                 :stopped-at (.plusSeconds initial-time 1800)}
                {:started-at (.plusSeconds initial-time 2900)
                 :stopped-at (.plusSeconds initial-time 5600)}]
               (time-span/adjust-time-spans-to-duration time-spans
                                                        4500
                                                        fake-current-time)))

        (is (= [{:started-at (.minusSeconds initial-time 1900)
                 :stopped-at (.plusSeconds initial-time 5600)}]
               (time-span/adjust-time-spans-to-duration time-spans
                                                        7500
                                                        fake-current-time)))

        (is (= []
               (time-span/adjust-time-spans-to-duration time-spans
                                                        0
                                                        fake-current-time))))))

  (testing "Adjusting a running set of time spans"
    (let [initial-time      (.minusSeconds (time/now) (* 3 24 60 60))
          time-spans        [{:started-at initial-time
                              :stopped-at (.plusSeconds initial-time 1800)}
                             {:started-at (.plusSeconds initial-time 3600)
                              :stopped-at nil}]
          fake-current-time (.plusSeconds initial-time 5600)]

      (with-redefs [time/now (constantly fake-current-time)]
        ;; TODO: Assert on the duration using a generative test.
        (is (= 1200
               (time-span/total-duration-in-seconds (time-span/adjust-time-spans-to-duration time-spans
                                                                                             1200
                                                                                             fake-current-time)
                                                    fake-current-time)))
        (is (= 1800
               (time-span/total-duration-in-seconds (time-span/adjust-time-spans-to-duration time-spans
                                                                                             1800
                                                                                             fake-current-time)
                                                    fake-current-time)))
        (is (= 4542
               (time-span/total-duration-in-seconds (time-span/adjust-time-spans-to-duration time-spans
                                                                                             4542
                                                                                             fake-current-time)
                                                    fake-current-time)))

        (is (= [{:started-at initial-time
                 :stopped-at (.plusSeconds initial-time 1800)}
                {:started-at fake-current-time
                 :stopped-at nil}]
               (time-span/adjust-time-spans-to-duration time-spans
                                                        1800
                                                        fake-current-time)))

        (is (= [{:started-at (.minusSeconds fake-current-time 1750)
                 :stopped-at nil}]
               (time-span/adjust-time-spans-to-duration time-spans
                                                        1750
                                                        fake-current-time)))

        (is (= [{:started-at initial-time
                 :stopped-at (.plusSeconds initial-time 1800)}
                {:started-at (.plusSeconds initial-time 2900)
                 :stopped-at nil}]
               (time-span/adjust-time-spans-to-duration time-spans
                                                        4500
                                                        fake-current-time)))

        (is (= [{:started-at (.minusSeconds initial-time 1900)
                 :stopped-at nil}]
               (time-span/adjust-time-spans-to-duration time-spans
                                                        7500
                                                        fake-current-time)))

        (is (= [{:started-at fake-current-time
                 :stopped-at nil}]
               (time-span/adjust-time-spans-to-duration time-spans
                                                        0
                                                        fake-current-time)))))))
