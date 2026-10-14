(ns pdns.pseudonym-test
  (:require [clojure.test :refer [deftest is testing]]
            [pdns.pseudonym :as pseudo]))

(def ^:private key-a "fixture-key-A-not-a-secret")
(def ^:private key-b "fixture-key-B-not-a-secret")

(deftest stable-within-key
  (is (= (pseudo/client-token key-a "203.0.113.7")
         (pseudo/client-token key-a "203.0.113.7"))))

(deftest changes-across-keys
  (is (not= (pseudo/client-token key-a "203.0.113.7")
            (pseudo/client-token key-b "203.0.113.7"))))

(deftest does-not-contain-raw-ip
  (let [tok (pseudo/client-token key-a "203.0.113.7")]
    (is (not (.contains ^String tok "203.0.113.7")))))

(deftest ipv6-accepted
  (is (string? (pseudo/client-token key-a "2001:db8::1"))))

(deftest rejects-malformed-ip
  (testing "garbage is refused, not hashed"
    (is (thrown? clojure.lang.ExceptionInfo (pseudo/client-token key-a "not-an-ip")))
    (is (thrown? clojure.lang.ExceptionInfo (pseudo/client-token key-a nil)))))

(deftest rejects-empty-key
  (is (thrown? clojure.lang.ExceptionInfo (pseudo/client-token "" "203.0.113.7"))))
