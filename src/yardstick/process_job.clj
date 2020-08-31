(ns yardstick.process-job
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [honeysql.core :as sql]
            [honeysql.helpers :refer [insert-into values]]
            [next.jdbc :as jdbc]))

(defn build-parser [attributes tenant-id]
  (fn [line]
    (reduce
     (fn [result {:keys [name csv-column parse]}]
       (assoc result name (parse (nth line csv-column))))
     {:tenant-id tenant-id}
     attributes)))

(defn parse-csv [file parse-fn]
  (with-open [reader (io/reader file)]
    (doall
     (->> (csv/read-csv reader)
          (drop 1)
          (map parse-fn)))))

; TODO batch
(defn build-insert-rows-sql [table rows]
  (sql/format (-> (insert-into table)
                  (values rows))))

(defn run-job [{:keys [attributes db-table]} file tenant-id ds]
  (let [parser (build-parser attributes tenant-id)
        rows (parse-csv file parser)
        sql (build-insert-rows-sql db-table rows)]
    (with-open [conn (jdbc/get-connection ds)]
      (jdbc/execute! conn sql))))
