(ns pdns.log
  "Build a log record from a query event under the validated config.

  The record never contains the raw :client-ip key from the input. The
  pseudonymised client token is always written. The raw IP is kept only
  when a caller explicitly sets :keep-raw-ip? (decision D7: raw IP lives
  for the same window as the record and is purged with it)."
  (:require [pdns.names :as names]
            [pdns.pseudonym :as pseudo]
            [pdns.config :as config]))

(defn- hash-fn-for [{:keys [hmac-key]}]
  (fn [n] (pseudo/hmac-token hmac-key n)))

(defn build-record
  "Return a log record map for one query event.

  event:  {:client-ip string, :query-name string, :ts java.time.Instant}
  opts:   {:cfg validated-config (from pdns.config/config),
           :hmac-key key (for client token and :hashed names),
           :psl       optional parsed PSL (pdns.names/parse-psl), default pinned list,
           :keep-raw-ip? boolean (default false)}"
  [{:keys [client-ip query-name ts]}
   {:keys [cfg hmac-key psl keep-raw-ip?]
    :or {keep-raw-ip? false}}]
  (let [cfg (config/config cfg)
        _ (when (or (nil? ts) (not (instance? java.time.Instant ts)))
            (throw (ex-info ":ts must be a java.time.Instant" {})))
        stored-name (names/apply-mode (:name-mode cfg) query-name
                                      {:hash-fn (hash-fn-for {:hmac-key hmac-key})
                                       :psl (or psl @names/default-psl)})
        base {:client-token (pseudo/client-token hmac-key client-ip)
              :name stored-name
              :name-mode (:name-mode cfg)
              :ts ts}]
    (cond-> base
      keep-raw-ip? (assoc :client-ip client-ip))))
