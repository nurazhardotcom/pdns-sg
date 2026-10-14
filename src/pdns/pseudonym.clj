(ns pdns.pseudonym
  "Keyed pseudonymisation (HMAC-SHA256) for client IPs and, optionally,
  query names under :hashed mode. The key is caller-supplied (decision D9)
  and never stored by this library."
  (:import (javax.crypto Mac)
           (javax.crypto.spec SecretKeySpec)
           (java.nio.charset StandardCharsets)
           (java.util Base64)))

(defn- ->key-bytes ^bytes [k]
  (cond
    (bytes? k) k
    (string? k) (.getBytes ^String k StandardCharsets/UTF_8)
    :else (throw (ex-info "HMAC key must be a string or byte array" {}))))

(defn hmac-token
  "Return a base64url HMAC-SHA256 token of `value` under `key`.
  Stable for a fixed key; changes when the key changes."
  [key value]
  (when (or (nil? value) (= "" value))
    (throw (ex-info "Value to pseudonymise is blank" {})))
  (let [kb (->key-bytes key)
        _ (when (zero? (alength kb))
            (throw (ex-info "HMAC key is empty" {})))
        mac (Mac/getInstance "HmacSHA256")
        _ (.init mac (SecretKeySpec. kb "HmacSHA256"))
        digest (.doFinal mac (.getBytes ^String value StandardCharsets/UTF_8))]
    (.encodeToString (.withoutPadding (Base64/getUrlEncoder)) digest)))

(defn client-token
  "Pseudonymise a client IP. Rejects anything that is not a plausible IPv4
  or IPv6 literal, so malformed input is never hashed silently."
  [key ip]
  (when-not (and (string? ip)
                 (or (re-matches #"\d{1,3}(\.\d{1,3}){3}" ip)
                     (re-matches #"[0-9a-fA-F:]+:[0-9a-fA-F:]*" ip)))
    (throw (ex-info "Client IP is not a valid IPv4 or IPv6 literal" {:ip-length (count (str ip))})))
  (hmac-token key ip))
