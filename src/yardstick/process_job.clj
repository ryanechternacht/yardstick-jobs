(ns yardstick.process-job
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [honeysql.core :as sql]
            [honeysql.helpers :refer [insert-into values]]
            [next.jdbc :as jdbc]
            [cheshire.core :as json]))

(defn- build-parser
  "Returns a function that takes a vector (normally a parsed line of a csv file) and
   extracts and validates the columns as defined in attributes. 
  
   Returns a map with :row defining the parsed values and :issues logging any issues 
   found while validating."
  [attributes tenant-id]
  (fn [line]
    (reduce
     (fn [result {:keys [col-name csv-column parse spec] :as col}]
       (try
         (let [parsed (parse (nth line csv-column))]
           (if (s/valid? spec parsed)
             (assoc-in result [:row col-name] parsed)
             (update result :issues conj col)))
         (catch Exception _ (update result :issues conj col))))
     {:row {:tenant-id tenant-id}
      :issues []}
     attributes)))

(defn- parse-csv [file parse-fn]
  (with-open [reader (io/reader file)]
    (doall
     (->> (csv/read-csv reader)
          (drop 1)
          (map parse-fn)
          (map-indexed (fn [i r] (assoc r :row-num i)))))))

(defn- build-insert-rows-sql [table rows]
  (sql/format (-> (insert-into table)
                  (values rows))))

(defn- insert-rows! [ds table rows]
  (with-open [conn (jdbc/get-connection ds)]
    (let [chunks (partition 100 100 [] rows)]
      (doseq [c chunks]
        (let [query (build-insert-rows-sql table c)]
          (jdbc/execute! conn query))))))

(defn- display-issue-rows [row]
  (let [{:keys [row-num issues]} row
        issues-simplified (map #(select-keys % [:col-name :csv-column]) issues)]
    {:row-num row-num
     :issues issues-simplified}))

(defn run-job [{:keys [attributes db-table]} params tenant-id ds]
  (let [{file :file} (json/parse-string params true)
        parser (build-parser attributes tenant-id)
        rows (parse-csv file parser)
        rows-for-db (->> rows
                         (filter #(empty? (:issues %)))
                         (map :row))
        rejected-rows (->> rows
                           (filter #(seq (:issues %)))
                           (map display-issue-rows))]
    (insert-rows! ds db-table rows-for-db)
    {:status "success"
     :accepted-count (count rows-for-db)
     :rejected-count (count rejected-rows)
     :rejected rejected-rows}))
