(ns yardstick.dispatch-jobs
  (:require [yardstick.process-job :as pj]
            [yardstick.jobs.sample-students :as ss]
            [yardstick.channels :as c]
            [clojure.core.async :refer [go >!]]
            [honeysql.helpers :refer [update sset merge-where] :rename {update h-update}]
            [honeysql.core :as hsql]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]))

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
  (pj/run-job (jobs name) params tenant-id ds))

(defn queue-done! [job result]
  (go
    (>! c/done {:job job :result result})))

(defn dispatch-job [job ds]
  (try
    (mark-job-as-running! job ds)
    (let [result (run-job! job ds)]
      (queue-done! job result))
    (catch Exception ex
      (prn ex))))
