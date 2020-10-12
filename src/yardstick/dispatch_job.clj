(ns yardstick.dispatch-job
  (:require [yardstick.jobs.sample-students :as ss]
            [yardstick.process-job :as pj]))

(def jobs {"sample-students|v1.0" ss/job})

(defn dispatch-job [job {:keys [file] :as params} tenant-id ds]
  (pj/run-job (jobs job) file tenant-id) ds)
