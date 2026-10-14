(ns pdns.config
  "Validated configuration for the privacy-by-design library.

  Defaults (decisions D3, D4):
    :name-mode      :etld1  (one of :etld1, :full, :hashed)
    :allow-full?    false   (must be true to use :full)
    :retention-days 7       (demo default, not a legal recommendation)")

(def defaults
  {:name-mode :etld1
   :allow-full? false
   :retention-days 7})

(def ^:private name-modes #{:etld1 :full :hashed})

(defn config
  "Merge caller options over defaults and validate. Throws ex-info on bad input."
  ([] (config {}))
  ([opts]
   (let [c (merge defaults opts)
         {:keys [name-mode allow-full? retention-days]} c]
     (when-not (contains? name-modes name-mode)
       (throw (ex-info "Unknown :name-mode" {:name-mode name-mode
                                              :expected name-modes})))
     (when-not (boolean? allow-full?)
       (throw (ex-info ":allow-full? must be a boolean" {:allow-full? allow-full?})))
     (when (and (= :full name-mode) (not allow-full?))
       (throw (ex-info "name-mode :full requires :allow-full? true"
                       {:name-mode name-mode})))
     (when-not (and (integer? retention-days) (pos? retention-days))
       (throw (ex-info ":retention-days must be a positive integer"
                       {:retention-days retention-days})))
     c)))
