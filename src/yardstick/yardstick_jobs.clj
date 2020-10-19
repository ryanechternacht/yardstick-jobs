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
  (when continue
    (let [job (<! c/todo)]
      (dj/dispatch-job job ds)
      (println "job ran")
      (println job))
    (recur @todo-loop)))


(def done-loop (atom true))

(reset! done-loop false)
(reset! done-loop true)

(go-loop [continue @done-loop]
  (when continue
    (let [result (<! c/done)]
      (println "got done")
      (println result)
      (dj/process-done result ds))
    (recur @done-loop)))
