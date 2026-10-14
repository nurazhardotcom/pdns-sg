(ns pdns.decision
  "Allow or block a query name against an in-memory blocklist (decision D8).

  The blocklist is a set of eTLD+1 strings supplied by the caller. A query
  whose registrable domain is blocked is blocked, so a subdomain of a
  blocked domain is also blocked. Source provenance (R5) is the caller's
  metadata, not read by this library."
  (:require [pdns.names :as names]))

(defn decide
  "Return {:action :allow|:block, :reason keyword, :domain string}.

  opts: {:blocklist set-of-etld1-strings, :psl optional parsed PSL}"
  [query-name {:keys [blocklist psl] :or {blocklist #{}}}]
  (let [n (names/normalise query-name)
        domain (names/etld1 n (or psl @names/default-psl))]
    (if (contains? blocklist domain)
      {:action :block :reason :blocklist-match :domain domain}
      {:action :allow :reason :not-listed :domain domain})))
