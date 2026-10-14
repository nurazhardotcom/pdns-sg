(ns pdns.names-test
  (:require [clojure.test :refer [deftest is testing]]
            [pdns.names :as names]))

(deftest normalise-names
  (is (= "www.example.test" (names/normalise "WWW.Example.Test.")))
  (is (= "example.test" (names/normalise "  example.test  ")))
  (is (thrown? clojure.lang.ExceptionInfo (names/normalise "   "))))

(deftest etld1-extraction
  (testing "subdomain collapses to registrable domain"
    (is (= "example.test" (names/etld1 "a.b.example.test"))))
  (testing "multi-label public suffix (fixture)"
    (is (= "example.com.sg" (names/etld1 "www.example.com.sg")))
    (is (= "example.co.uk" (names/etld1 "shop.example.co.uk"))))
  (testing "name already at eTLD+1 is unchanged"
    (is (= "example.test" (names/etld1 "example.test")))))

(deftest apply-mode-dispatch
  (testing ":etld1 stores registrable domain"
    (is (= "example.test" (names/apply-mode :etld1 "clinic.example.test"))))
  (testing ":full stores normalised full name"
    (is (= "clinic.example.test" (names/apply-mode :full "Clinic.Example.Test."))))
  (testing ":hashed uses the supplied hash fn"
    (is (= "H(clinic.example.test)"
           (names/apply-mode :hashed "clinic.example.test"
                             {:hash-fn #(str "H(" % ")")}))))
  (testing ":hashed without hash-fn throws"
    (is (thrown? clojure.lang.ExceptionInfo
                 (names/apply-mode :hashed "example.test")))))
