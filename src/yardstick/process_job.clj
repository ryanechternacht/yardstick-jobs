(ns yardstick.process-job
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]))

(defn build-parser [columns]
  (fn [line]
    (reduce
     (fn [result {:keys [name csv-column parse]}]
       (assoc result name (parse (nth line csv-column))))
     {}
     columns)))

(defn parse-csv [file parse-fn]
  (with-open [reader (io/reader file)]
    (doall
     (->> (csv/read-csv reader)
          (drop 1)
          (map parse-fn)))))

(defn run-job [{:keys [columns]} file]
  (let [parser (build-parser columns)]))