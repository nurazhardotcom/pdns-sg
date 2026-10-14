(ns pdns.decision-test
  (:require [clojure.test :refer [deftest is testing]]
            [pdns.decision :as decision]))

(def ^:private blocklist #{"example.test"})

(deftest blocks-listed-domain
  (is (= {:action :block :reason :blocklist-match :domain "example.test"}
         (decision/decide "example.test" {:blocklist blocklist}))))

(deftest blocks-subdomain-of-listed-domain
  (testing "a subdomain collapses to its eTLD+1 and is blocked"
    (is (= :block (:action (decision/decide "a.b.example.test" {:blocklist blocklist}))))))

(deftest allows-unlisted-domain
  (is (= {:action :allow :reason :not-listed :domain "other.test"}
         (decision/decide "other.test" {:blocklist blocklist}))))

(deftest empty-blocklist-allows-everything
  (is (= :allow (:action (decision/decide "example.test" {})))))

(deftest rejects-blank-name
  (is (thrown? clojure.lang.ExceptionInfo
               (decision/decide "  " {:blocklist blocklist}))))
