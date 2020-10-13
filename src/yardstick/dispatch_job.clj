(ns yardstick.dispatch-job
  (:require [yardstick.jobs.sample-students :as ss]
            [yardstick.process-job :as pj]
            [honeysql.core :as hsql]
            [honeysql.helpers :as h]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]))

(def jobs {"sample-students|v1.0" ss/job})

(defn dispatch-job [job {:keys [file] :as params} tenant-id ds]
  (pj/run-job (jobs job) file tenant-id) ds)

(defn fetch-jobs [ds max-capacity]
  (prn "fetch-jobs")
  (with-open [connection (jdbc/get-connection ds)]
    (let [query (-> (h/select :id :tenant_id :name :params)
                    (h/from :job)
                    (h/merge-where [:= :status "pending"])
                    (h/order-by [:queued_at])
                    (h/limit max-capacity))]
      (->> query
           (hsql/format)
           (sql/query connection)))))
