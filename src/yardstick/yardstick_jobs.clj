(ns yardstick.yardstick-jobs
  (:gen-class)
  (:require [next.jdbc :as jdbc]
            [yardstick.jobs.sample-students :as ss]
            [yardstick.process-job :as pj]
            ;; [mount.core :as mount]
            )
  (:import
   (java.util.concurrent Executors ScheduledExecutorService ThreadFactory TimeUnit)))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

(def db-conn {:dbtype "mysql"
              :dbname "yardstick"
              :user "root"
              :password "root"
              :host "127.0.0.1"
              :port 8889})

(def ds (jdbc/get-datasource db-conn))

;; (pj/run-job ss/job "resources/sample-students.csv" 1 ds)

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
  (if (.isShutdown executor)
    (prn "Executor already shut down")
    (do
      (prn "Shutting down executor")
      (.shutdown executor)                                  ;; Disable new tasks from being scheduled
      (try
        ;; Wait a while for currently running tasks to finish
        (if-not (.awaitTermination executor 10 TimeUnit/SECONDS)
          (do
            (.shutdownNow executor)                         ;; Cancel currently running tasks
            (prn "Still waiting to shut down executor. Sending interrupt to tasks.")
            ;; Wait a while for tasks to respond to being cancelled
            (when-not (.awaitTermination executor 10 TimeUnit/SECONDS)
              (throw (ex-info "Executor could not be shut down" {}))))
          (prn "Executor shutdown completed"))
        (catch InterruptedException _
          (prn "Interrupted while shutting down. Sending interrupt to tasks.")
          ;; Re-cancel if current thread also interrupted
          (.shutdownNow executor)
          ;; Preserve interrupt status
          (.interrupt (Thread/currentThread)))))))

(defn run-job
  []
  (pj/run-job ss/job "resources/sample-students.csv" 1 ds))

(def executor (create-single-thread-scheduled-executor "world-listener"))

(schedule executor run-job 10 TimeUnit/SECONDS)

(.shutdown executor)

;; (mount/defstate world-listener
;;   :start (doto (create-single-thread-scheduled-executor "world-listener")
;;            (schedule world-updating-fn 10 TimeUnit/SECONDS))
;;   :stop (shutdown-executor world-listener))
