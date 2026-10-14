(ns pdns.names-test
  (:require [clojure.test :refer [deftest is testing]]
            [pdns.names :as names]))

(deftest normalise-names
  (is (= "www.example.test" (names/normalise "WWW.Example.Test.")))
  (is (= "example.test" (names/normalise "  example.test  ")))
  (is (thrown? clojure.lang.ExceptionInfo (names/normalise "   "))))

(deftest parse-psl-rule-types
  (let [psl (names/parse-psl "// comment\n\ncom\n*.ck\n!www.ck\n")]
    (is (= #{"com"} (:rules psl)))
    (is (= #{"ck"} (:wild psl)))
    (is (= #{"www.ck"} (:exceptions psl)))))

(deftest etld1-with-pinned-list
  (testing "plain suffix: multi-label ICANN entries"
    (is (= "example.co.uk" (names/etld1 "shop.example.co.uk")))
    (is (= "example.com.sg" (names/etld1 "www.example.com.sg"))))
  (testing "private-section suffix (github.io) is a public suffix"
    (is (= "alice.github.io" (names/etld1 "www.alice.github.io")))
    (is (= "alice.github.io" (names/etld1 "alice.github.io"))))
  (testing "name at eTLD+1 is unchanged"
    (is (= "example.test" (names/etld1 "example.test"))))
  (testing "a bare public suffix is returned unchanged"
    (is (= "co.uk" (names/etld1 "co.uk")))))

(deftest etld1-wildcard-and-exception
  (testing "wildcard *.ck: one extra label is part of the suffix"
    (is (= "foo.bar.ck" (names/etld1 "x.foo.bar.ck"))))
  (testing "exception !www.ck: www.ck is the registrable domain"
    (is (= "www.ck" (names/etld1 "a.www.ck")))))

(deftest etld1-unknown-tld-uses-implicit-rule
  (is (= "example.invalid" (names/etld1 "a.b.example.invalid"))))

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
