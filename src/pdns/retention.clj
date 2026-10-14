(ns pdns.retention
  "Retention decisions for log records. Pure: the caller supplies `now`.

  A record is kept while its age is strictly less than the window. A record
  whose age equals the window exactly is purged (decision D4, default 7 days).
  Purging a record also removes any raw :client-ip (decision D7)."
  (:import (java.time Duration Instant)))

(defn- age-ok?
  "True when the record's age is strictly less than the window."
  [now ts window]
  (let [age (Duration/between ts now)]
    (< (.compareTo age window) 0)))

(defn partition-by-age
  "Split records into {:keep [...] :purge [...]} relative to `now`.
  window-days is a positive integer."
  [records now window-days]
  (let [window (Duration/ofDays window-days)]
    (reduce
     (fn [acc r]
       (let [ts (:ts r)]
         (when-not (instance? Instant ts)
           (throw (ex-info "Record :ts must be an Instant" {})))
          (if (age-ok? now ts window)
           (update acc :keep conj r)
           (update acc :purge conj r))))
     {:keep [] :purge []}
     records)))

(defn purge-record
  "Remove the raw IP field from a record. Used when a record is retained
  but its raw IP is past the window (defensive; normally the whole record
  is purged)."
  [record]
  (dissoc record :client-ip))
