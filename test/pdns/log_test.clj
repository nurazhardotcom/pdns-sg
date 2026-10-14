(ns pdns.log-test
  (:require [clojure.test :refer [deftest is testing]]
            [pdns.log :as log])
  (:import (java.time Instant)))

(def ^:private key-a "fixture-key-A-not-a-secret")
(def ^:private ts (Instant/parse "2026-10-14T00:00:00Z"))
(def ^:private event {:client-ip "203.0.113.7"
                      :query-name "Clinic.Example.Test."
                      :ts ts})

(deftest default-record-has-no-raw-ip
  (let [rec (log/build-record event {:cfg {} :hmac-key key-a})]
    (is (= "example.test" (:name rec)))
    (is (= :etld1 (:name-mode rec)))
    (is (contains? rec :client-token))
    (is (not (contains? rec :client-ip)))
    (is (not (.contains (pr-str rec) "203.0.113.7")))))

(deftest raw-ip-only-when-explicitly-kept
  (let [rec (log/build-record event {:cfg {} :hmac-key key-a :keep-raw-ip? true})]
    (is (= "203.0.113.7" (:client-ip rec)))))

(deftest full-mode-needs-opt-in
  (testing "without opt-in the config throws"
    (is (thrown? clojure.lang.ExceptionInfo
                 (log/build-record event {:cfg {:name-mode :full}
                                          :hmac-key key-a}))))
  (testing "with opt-in the full name is stored"
    (let [rec (log/build-record event {:cfg {:name-mode :full :allow-full? true}
                                       :hmac-key key-a})]
      (is (= "clinic.example.test" (:name rec))))))

(deftest hashed-mode-stores-keyed-token-not-name
  (let [rec (log/build-record event {:cfg {:name-mode :hashed}
                                     :hmac-key key-a})]
    (is (not= "example.test" (:name rec)))
    (is (not (.contains ^String (:name rec) "clinic")))))

(deftest rejects-missing-timestamp
  (is (thrown? clojure.lang.ExceptionInfo
               (log/build-record (dissoc event :ts) {:cfg {} :hmac-key key-a}))))
