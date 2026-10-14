(ns pdns.config-test
  (:require [clojure.test :refer [deftest is testing]]
            [pdns.config :as config]))

(deftest defaults
  (testing "defaults match decisions D3 and D4"
    (is (= {:name-mode :etld1 :allow-full? false :retention-days 7}
           (config/config)))))

(deftest full-requires-opt-in
  (testing ":full without :allow-full? throws"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"requires :allow-full"
                          (config/config {:name-mode :full}))))
  (testing ":full with opt-in is accepted"
    (is (= :full (:name-mode (config/config {:name-mode :full :allow-full? true}))))))

(deftest rejects-bad-values
  (is (thrown? clojure.lang.ExceptionInfo (config/config {:name-mode :bogus})))
  (is (thrown? clojure.lang.ExceptionInfo (config/config {:retention-days 0})))
  (is (thrown? clojure.lang.ExceptionInfo (config/config {:retention-days 1.5})))
  (is (thrown? clojure.lang.ExceptionInfo (config/config {:allow-full? "yes"}))))
