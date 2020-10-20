(ns yardstick.yardstick-jobs
  (:gen-class)
  (:require
   [mount.core :as mount]
   [yardstick.scheduler :as s]
   [yardstick.fetch-jobs :as fj]
   [yardstick.dispatch-jobs :as dj]
   [yardstick.channels :as c]
   [clojure.core.async :refer [<! >! go chan go-loop]]
   [next.jdbc :as jdbc]))

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

(s/mount-scheduler #(fj/fetch-and-add-jobs ds))

(mount/start)

(mount/stop)

(def todo-loop (atom true))

(reset! todo-loop false)
(reset! todo-loop true)

(go-loop [continue @todo-loop]
  (let [job (<! c/todo)]
    (if continue
      (do (dj/dispatch-job job ds)
          (println "job ran")
          (println job)
          (recur @todo-loop))
      (>! c/todo job))))


(def done-loop (atom true))

(reset! done-loop false)
(reset! done-loop true)

(go-loop [continue @done-loop]
  (let [result (<! c/done)]
    (if continue
      (do
        (println "got done")
        (println result)
        (dj/process-done! result ds)
        (recur @done-loop))
      (>! c/done result))))


(def error-loop (atom true))

(reset! error-loop false)
(reset! error-loop true)

(go-loop [continue @error-loop]
  (let [result (<! c/error)]
    (if continue
      (do
        (println "got error")
        (println result)
        (dj/process-error! result ds)
        (recur @error-loop))
      (>! c/error result))))