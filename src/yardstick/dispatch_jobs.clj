(ns yardstick.dispatch-jobs
  (:require [yardstick.process-job :as pj]
            [yardstick.jobs.sample-students :as ss]))

(def jobs {"sample-students|v1.0" ss/job})

(defn dispatch-job [{name :job/name params :job/params tenant-id :job/tenant_id} ds]
  (pj/run-job (jobs name) params tenant-id ds))
