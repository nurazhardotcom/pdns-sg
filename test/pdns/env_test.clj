(ns pdns.env-test
  (:require [clojure.test :refer [deftest is]]
            [pdns.env :as env]))

(deftest missing-variable-throws-without-leaking
  (let [var "PDNS_TEST_UNSET_VAR_XYZ"]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"is not set or empty"
                          (env/hmac-key-from-env var)))))
