(ns chronograph-web.utils.fetch-events-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests]]
            [chronograph-web.utils.fetch-events :as fetch-events]))

(deftest http-request-creator-test
  (testing "It should inject the success and failure events if present"
    (let [event-handler (fetch-events/http-request-creator
                         (fn [{:keys [path] :as _cofx} [_ other-path]]
                           {:http-xhrio {:uri        (str "/api/timers/" path "/" other-path)
                                         :on-success [:event-success]
                                         :on-failure [:event-fail]}}))]
      (is (= {:http-xhrio {:uri        "/api/timers/cofx/baz"
                           :on-success [:event-success [:foobar "baz"]]
                           :on-failure [:quux]}}
             (event-handler {:path "cofx"}
                            [:event-name "baz" {:on-success [:foobar "baz"]
                                                :on-failure [:quux]}])))))

  (testing "It should inject only the success event if present"
    (let [event-handler (fetch-events/http-request-creator
                         (fn [{:keys [path] :as _cofx} [_ other-path]]
                           {:http-xhrio {:uri        (str "/api/timers/" path "/" other-path)
                                         :on-success [:event-success]
                                         :on-failure [:event-fail]}}))]
      (is (= {:http-xhrio {:uri        "/api/timers/cofx/baz"
                           :on-success [:event-success [:foobar "baz"]]
                           :on-failure [:event-fail]}}
             (event-handler {:path "cofx"}
                            [:event-name "baz" {:on-success [:foobar "baz"]}])))))

  (testing "It should inject nil in on-success if no hooks are given"
    (let [event-handler (fetch-events/http-request-creator
                         (fn [{:keys [path] :as _cofx} [_ other-path]]
                           {:http-xhrio {:uri        (str "/api/timers/" path "/" other-path)
                                         :on-success [:event-success]
                                         :on-failure [:event-fail]}}))]
      (is (= {:http-xhrio {:uri        "/api/timers/cofx/baz"
                           :on-success [:event-success nil]
                           :on-failure [:event-fail]}}
             (event-handler {:path "cofx"}
                            [:event-name "baz"]))))))

(deftest http-success-handler-test
  (testing "when a post-success event is present"
    (let [event-handler (fetch-events/http-success-handler
                         (fn [{:keys [db]} [_ resp]]
                           {:db (assoc db :resp resp)
                            :fx [[:dispatch [:some "thing"]]]}))]
      (is (= {:db {:resp "resp"}
              :fx [[:dispatch [:some "thing"]]
                   [:dispatch [:on-success "foobar" "resp"]]]}
             (event-handler {:db {}}
                            [:baz [:on-success "foobar"] "resp"]))
          "it should inject the post-success dispatch")))

  (testing "when the post-success event is nil"
    (let [event-handler (fetch-events/http-success-handler
                         (fn [{:keys [db]} [_ resp]]
                           {:db (assoc db :resp resp)
                            :fx [[:dispatch [:some "thing"]]]}))]
      (is (= {:db {:resp "resp"}
              :fx [[:dispatch [:some "thing"]]]}
             (event-handler {:db {}}
                            [:baz nil "resp"]))
          "it should not inject the post-success dispatch")))

  (testing "when the post-success event is nil and no fx are returned by the supplied handler"
    (let [event-handler (fetch-events/http-success-handler
                         (fn [{:keys [db]} [_ resp]]
                           {:db (assoc db :resp resp)}))]
      (is (= {:db {:resp "resp"}
              :fx []}
             (event-handler {:db {}}
                            [:baz nil "resp"]))
          "it should not dispatch any effect"))))
