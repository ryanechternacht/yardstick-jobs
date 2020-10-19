(ns yardstick.dispatch-jobs
  (:require [yardstick.process-job :as pj]
            [yardstick.jobs.sample-students :as ss]
            [honeysql.helpers :refer [update sset merge-where] :rename {update h-update}]
            [honeysql.core :as hsql]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]))

(def jobs {"sample-students|v1.0" ss/job})

(defn mark-job-as-running [{id :job/id} ds]
  (with-open [conn (jdbc/get-connection ds)]
    (let [query (-> (h-update :job)
                    (sset {:status "running"})
                    (merge-where [:= :id id]))]
      (->> query
           (hsql/format)
           (sql/query conn)))))

(defn dispatch-job [{name :job/name params :job/params tenant-id :job/tenant_id :as job} ds]
  (mark-job-as-running job ds)
  (pj/run-job (jobs name) params tenant-id ds))
