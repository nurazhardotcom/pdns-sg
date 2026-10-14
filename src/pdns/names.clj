(ns pdns.names
  "Query-name handling: normalisation, eTLD+1 extraction, and mode dispatch.

  eTLD+1 follows the Public Suffix List algorithm
  (https://publicsuffix.org/list/): the longest matching rule wins, wildcard
  rules (`*.ck`) match one extra label, and exception rules (`!www.ck`)
  override a wildcard. The list is loaded from resources/psl/public_suffix_list.dat
  (pinned; version recorded in the file header). Callers may pass their own
  rule set for testing or newer lists."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(defn parse-psl
  "Parse PSL text into {:rules #{...} :wild #{...} :exceptions #{...}}.
  Comment lines (starting with //) and blank lines are ignored. Rules are
  lowercase, without the leading `*.` or `!`."
  [text]
  (reduce
   (fn [acc line]
     (let [line (str/trim line)]
       (cond
         (or (str/blank? line) (str/starts-with? line "//")) acc
         (str/starts-with? line "!") (update acc :exceptions conj (subs line 1))
         (str/starts-with? line "*.") (update acc :wild conj (subs line 2))
         :else (update acc :rules conj line))))
   {:rules #{} :wild #{} :exceptions #{}}
   (str/split-lines text)))

(def ^:private psl-resource "psl/public_suffix_list.dat")

(def default-psl
  "The pinned Public Suffix List, parsed once at load time."
  (delay
    (if-let [r (io/resource psl-resource)]
      (parse-psl (slurp r))
      (throw (ex-info "Pinned public suffix list not found on classpath"
                      {:resource psl-resource})))))

(defn normalise
  "Lowercase, strip a trailing dot, and trim. Rejects blank names."
  [^String s]
  (when (or (nil? s) (str/blank? s))
    (throw (ex-info "Query name is blank" {})))
  (-> s str/trim str/lower-case (str/replace #"\.$" "")))

(defn- public-suffix-len
  "Number of labels in the public suffix of `labels` (a vector, leftmost first),
  per the PSL algorithm. Unknown TLDs use the implicit `*` rule (length 1)."
  [labels {:keys [rules wild exceptions]}]
  (let [n (count labels)
        ;; candidate suffix lengths that match a rule, from the right
        matches
        (for [i (range n)
              :let [len (- n i)
                    suffix (str/join "." (drop i labels))
                    parent (when (> len 1) (str/join "." (drop (inc i) labels)))]
              :when (or (contains? rules suffix)
                        (and parent (contains? wild parent)))]
          (if (contains? exceptions suffix)
            (dec len)
            len))
        best (when (seq matches) (apply max matches))]
    (or best 1)))

(defn etld1
  "Return the registrable domain (eTLD+1) for a normalised name.
  Returns the name itself when it is already a public suffix or has no
  label above one."
  ([n] (etld1 n @default-psl))
  ([n psl]
   (let [labels (vec (str/split n #"\."))
         suffix-len (public-suffix-len labels psl)
         keep (inc suffix-len)]
     (if (> (count labels) suffix-len)
       (str/join "." (take-last keep labels))
       n))))

(defn apply-mode
  "Return the stored form of a query name under a validated config mode.
  :etld1  -> registrable domain
  :full   -> normalised full name (caller must have opted in via config)
  :hashed -> keyed HMAC of the full name, via the supplied hash fn

  opts: {:hash-fn f, :psl parsed-psl}"
  ([mode n] (apply-mode mode n {}))
  ([mode n {:keys [hash-fn psl] :or {psl @default-psl}}]
   (let [n (normalise n)]
     (case mode
       :etld1  (etld1 n psl)
       :full   n
       :hashed (do (when-not hash-fn
                     (throw (ex-info ":hashed mode needs :hash-fn" {})))
                   (hash-fn n))))))
