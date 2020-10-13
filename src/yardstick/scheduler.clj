(ns yardstick.scheduler
  (:require [mount.core :as mount]
            [yardstick.dispatch-job :as dj]
            [yardstick.jobs.sample-students :as ss])
  (:import
   (java.util.concurrent Executors ScheduledExecutorService ThreadFactory TimeUnit)))

(defn ^ThreadFactory create-thread-factory
  [thread-name-prefix]
  (let [thread-number (atom 0)]
    (reify ThreadFactory
      (newThread [_ runnable]
        (Thread. runnable (str thread-name-prefix "-" (swap! thread-number inc)))))))

(defn ^ScheduledExecutorService create-single-thread-scheduled-executor
  [thread-name-prefix]
  (let [thread-factory (create-thread-factory thread-name-prefix)]
    (Executors/newSingleThreadScheduledExecutor thread-factory)))

(defn schedule [executor runnable interval unit]
  (.scheduleWithFixedDelay executor runnable 0 interval unit))

(defn shutdown-executor
  "Industrial-strength executor shutdown, modify/simplify according to need."
  [^ScheduledExecutorService executor]
  (.shutdown executor)
  (if-not (.awaitTermination executor 10 TimeUnit/SECONDS)
    (do
      (.shutdownNow executor)
      (prn "Still waiting to shut down executor. Sending interrupt to tasks.")
      (when-not (.awaitTermination executor 10 TimeUnit/SECONDS)
        (throw (ex-info "Executor could not be shut down" {}))))
    (prn "Executor shutdown completed")))

(defn- do-work
  [ds]
  (prn (dj/fetch-jobs ds 1)))

(defn mount-state
  [ds]
  (mount/defstate job-manager
    :start (doto (create-single-thread-scheduled-executor "job-manager")
             (schedule #(do-work ds) 10 TimeUnit/SECONDS))
    :stop (shutdown-executor job-manager)))
