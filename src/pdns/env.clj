(ns pdns.env
  "Optional helper that reads the HMAC key from an environment variable
  (decision D9). This is the only native adapter in the library.

  Default variable name: PDNS_HMAC_KEY. The key is never logged or
  returned in an error message."
  (:require [pdns.pseudonym :as pseudo]))

(def default-var "PDNS_HMAC_KEY")

(defn hmac-key-from-env
  "Return the key from env var `var-name` (default PDNS_HMAC_KEY).
  Throws without including the key value in the message."
  ([] (hmac-key-from-env default-var))
  ([var-name]
   (let [v (System/getenv var-name)]
     (when (or (nil? v) (= "" v))
       (throw (ex-info (str "Environment variable " var-name " is not set or empty")
                       {:var var-name})))
     v)))

(defn client-token-from-env
  "Pseudonymise an IP using the key from the environment."
  [ip]
  (pseudo/client-token (hmac-key-from-env) ip))
