# pdns-sg

Privacy-by-design library for protective DNS data handling in Singapore.

This is a portfolio project. It is a Clojure library with no network service and no DNS protocol code. It implements PDPA-aware handling of DNS query data and documents each control against a PDPA obligation in [`docs/design.md`](docs/design.md). It complements [pdpa-sg-clj](https://github.com/nurazhardotcom/pdpa-sg-clj).

## What it does

- Pseudonymises client IPs with HMAC-SHA256 under a caller-supplied key.
- Stores query names at registrable-domain level (eTLD+1) by default. Full names require an explicit opt-in. A keyed-hash mode is available.
- Builds log records that never contain a raw client IP unless the caller opts in.
- Purges log records older than a configurable window (default 7 days, a demo default and not a legal recommendation).
- Decides allow or block for a query name against an in-memory blocklist.

## Usage

```clojure
(require '[pdns.config :as config]
         '[pdns.log :as log]
         '[pdns.retention :as retention]
         '[pdns.decision :as decision])

(def cfg (config/config {:name-mode :etld1 :retention-days 7}))

(def record
  (log/build-record {:client-ip "203.0.113.7"
                     :query-name "clinic.example.test"
                     :ts (java.time.Instant/now)}
                    {:cfg cfg :hmac-key "caller-supplied-key"}))
;; => {:client-token "..." :name "example.test" :name-mode :etld1 :ts ...}

(decision/decide "clinic.example.test" {:blocklist #{"example.test"}})
;; => {:action :block :reason :blocklist-match :domain "example.test"}
```

The HMAC key is never stored by the library. For the demo, `pdns.env/hmac-key-from-env` reads `PDNS_HMAC_KEY`.

## Test

```sh
bb test                   # Babashka
clojure -M:test           # JVM (test-runner alias)
```

All test data is fictional: `203.0.113.0/24`, `2001:db8::/32`, `example.test`.

## Limits

- The public-suffix list is a small fixture for demos. Production must supply the full PSL.
- The library does not forward DNS queries. Upstream transfer analysis (PDPA s26) is the caller's responsibility, covered in the design doc.
- Not legal advice. The design doc is engineering guidance.

## License

MIT, to match pdpa-sg-clj.
