(ns yardstick.process-job
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [honeysql.core :as sql]
            [honeysql.helpers :refer [insert-into values]]
            [next.jdbc :as jdbc]))

(defn build-parser [attributes tenant-id]
  (fn [line]
    (reduce
     (fn [result {:keys [col-name csv-column parse spec] :as col}]
       (try
         (let [parsed (parse (nth line csv-column))]
           (if (s/valid? spec parsed)
             (assoc result col-name parsed)
             (update result :issues conj col)))
         (catch Exception _ (update result :issues conj col))))
     {:tenant-id tenant-id
      :issues []}
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

(defn insert-rows! [ds table rows]
  (with-open [conn (jdbc/get-connection ds)]
    (let [chunks (partition 100 100 [] rows)]
      (doseq [c chunks]
        (prn c)
        (let [sql (build-insert-rows-sql table c)]
          (jdbc/execute! conn sql))))))

(defn run-job [{:keys [attributes db-table]} file tenant-id ds]
  (let [parser (build-parser attributes tenant-id)
        rows (parse-csv file parser)
        rows-for-db (->> rows
                         (filter #(empty? (:issues %)))
                         (map #(dissoc % :issues)))]
    (insert-rows! ds db-table rows-for-db)))
