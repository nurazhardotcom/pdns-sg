(ns pdns.names
  "Query-name handling: normalisation, eTLD+1 extraction, and mode dispatch.

  The public-suffix list is caller data (decision 9.2). A small fixed
  fixture is embedded for tests and demos; production must supply the full
  PSL. Names are normalised to lowercase ASCII (punycode) without a trailing dot."
  (:require [clojure.string :as str]))

(def fixture-suffixes
  "Demo fixture only. Not the full Public Suffix List."
  #{"com" "org" "net" "edu" "gov" "sg" "com.sg" "edu.sg" "gov.sg" "net.sg"
    "test" "example" "uk" "co.uk"})

(defn normalise
  "Lowercase, strip a trailing dot, and trim. Rejects blank names."
  [^String s]
  (when (or (nil? s) (str/blank? s))
    (throw (ex-info "Query name is blank" {})))
  (-> s str/trim str/lower-case (str/replace #"\.$" "")))

(defn etld1
  "Return the registrable domain (eTLD+1) for a normalised name using the
  supplied suffix set. Returns the name itself when it has no label above
  a known suffix."
  ([n] (etld1 n fixture-suffixes))
  ([n suffixes]
   (let [labels (str/split n #"\.")
         ;; longest suffix match from the left
         suffix-len (loop [i 0]
                      (cond
                        (>= i (count labels)) 1
                        (contains? suffixes (str/join "." (drop i labels)))
                        (- (count labels) i)
                        :else (recur (inc i))))
         keep (inc suffix-len)]
     (if (> (count labels) keep)
       (str/join "." (take-last keep labels))
       n))))

(defn apply-mode
  "Return the stored form of a query name under a validated config mode.
  :etld1  -> registrable domain
  :full   -> normalised full name (caller must have opted in via config)
  :hashed -> keyed HMAC of the full name, via the supplied hash fn"
  ([mode n] (apply-mode mode n {}))
  ([mode n {:keys [hash-fn suffixes] :or {suffixes fixture-suffixes}}]
   (let [n (normalise n)]
     (case mode
       :etld1  (etld1 n suffixes)
       :full   n
       :hashed (do (when-not hash-fn
                     (throw (ex-info ":hashed mode needs :hash-fn" {})))
                   (hash-fn n))))))
