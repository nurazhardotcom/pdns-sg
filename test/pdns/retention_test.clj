(ns pdns.retention-test
  (:require [clojure.test :refer [deftest is testing]]
            [pdns.retention :as ret])
  (:import (java.time Instant)))

(def ^:private now (Instant/parse "2026-10-14T00:00:00Z"))

(defn- ago [days]
  (.minusSeconds now (* days 86400)))

(deftest keeps-within-window
  (let [recs [{:id 1 :ts (ago 1)} {:id 2 :ts (ago 6)}]
        {:keys [keep purge]} (ret/partition-by-age recs now 7)]
    (is (= [1 2] (map :id keep)))
    (is (empty? purge))))

(deftest purges-at-and-past-window-edge
  (testing "exactly 7 days old is purged (age must be strictly less)"
    (let [{:keys [keep purge]} (ret/partition-by-age [{:id 3 :ts (ago 7)}] now 7)]
      (is (empty? keep))
      (is (= [3] (map :id purge)))))
  (testing "older than window is purged"
    (let [{:keys [purge]} (ret/partition-by-age [{:id 4 :ts (ago 30)}] now 7)]
      (is (= [4] (map :id purge))))))

(deftest rejects-non-instant-timestamp
  (is (thrown? clojure.lang.ExceptionInfo
               (ret/partition-by-age [{:ts "2026-10-01"}] now 7))))

(deftest purge-record-drops-raw-ip
  (is (= {:name "example.test"}
         (ret/purge-record {:name "example.test" :client-ip "203.0.113.7"}))))
