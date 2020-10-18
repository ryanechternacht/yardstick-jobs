(ns yardstick.fetch-jobs
  (:require [honeysql.core :as hsql]
            [honeysql.helpers :refer [select from merge-where order-by limit]]
            [next.jdbc :as jdbc]
            [yardstick.channels :as c]
            [clojure.core.async :refer [go >!]]
            [next.jdbc.sql :as sql]))

(def ^:private last-id-seen (atom -1))

(defn fetch-jobs [ds max-capacity last-seen-id]
  (with-open [connection (jdbc/get-connection ds)]
    (let [query (-> (select :id :tenant_id :name :params)
                    (from :job)
                    (merge-where [:= :status "pending"])
                    (merge-where [:> :id last-seen-id])
                    (order-by [:queued_at])
                    (limit max-capacity))]
      (->> query
           (hsql/format)
           (sql/query connection)))))

(defn add-jobs [jobs chan last-id-seen-atom]
  (when (not-empty jobs)
    (go
      (doseq [j jobs]
        (>! chan j)))
    (reset! last-id-seen-atom (:job/id (last jobs)))))

(defn fetch-and-add-jobs [ds]
  (let [capacity (- c/max-jobs-in-system @c/jobs-in-system)]
    (when (> capacity 0)
      (-> (fetch-jobs ds capacity @last-id-seen)
          ((fn [x] (println "jobs to run") (println x) x))
          (add-jobs c/todo last-id-seen)))))