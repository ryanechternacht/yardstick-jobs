(ns yardstick.dispatch-jobs
  (:require [yardstick.process-job :as pj]
            [yardstick.jobs.sample-students :as ss]
            [yardstick.channels :as c]
            [clojure.core.async :refer [go >!]]
            [honeysql.helpers :refer [update sset merge-where] :rename {update h-update}]
            [honeysql.core :as hsql]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [cheshire.core :as json]))

(def jobs {"sample-students|v1.0" ss/job})

(defn mark-job-as-running! [{id :job/id} ds]
  (with-open [conn (jdbc/get-connection ds)]
    (let [query (-> (h-update :job)
                    (sset {:status "running"
                           :started_at :%CURRENT_TIMESTAMP})
                    (merge-where [:= :id id]))]
      (->> query
           (hsql/format)
           (sql/query conn)))))

(defn run-job! [{name :job/name params :job/params tenant-id :job/tenant_id} ds]
  (/ 1 0)
  (pj/run-job (jobs name) params tenant-id ds))

(defn queue-done! [job result]
  (go
    (>! c/done {:job job :result result})))

(defn process-done! [{result :result {id :job/id} :job} ds]
  (with-open [conn (jdbc/get-connection ds)]
    (let [completion-report (json/generate-string result)
          query (-> (h-update :job)
                    (sset {:status "done"
                           :completed_at :%CURRENT_TIMESTAMP
                           :completition_report completion-report})
                    (merge-where [:= :id id]))]
      (->> query
           (hsql/format)
           (sql/query conn)))))

(defn queue-error! [job ex]
  (go
    (>! c/error {:job job :error ex})))

(defn process-error! [{error :error {id :job/id} :job} ds]
  (with-open [conn (jdbc/get-connection ds)]
    (let [error-json (json/generate-string error)
          query (-> (h-update :job)
                    (sset {:status "error"
                           :completed_at :%CURRENT_TIMESTAMP
                           :completition_report error-json})
                    (merge-where [:= :id id]))]
      (->> query
           (hsql/format)
           (sql/query conn)))))

(defn dispatch-job [job ds]
  (try
    (mark-job-as-running! job ds)
    (let [result (run-job! job ds)]
      (queue-done! job result))
    (catch Exception ex
      (queue-error! job {:error (.toString ex)}))))
