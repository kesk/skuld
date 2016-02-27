(ns skuld.common)

(defn get-element [id]
  (.getElementById js/document id))
