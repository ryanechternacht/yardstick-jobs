(ns yardstick.yardstick-jobs
  (:gen-class)
  (:require
   [mount.core :as mount]
   [yardstick.scheduler :as s]
   [yardstick.fetch-jobs :as fj]
   [yardstick.channels :as c]
   [clojure.core.async :refer [<! >! go chan]]
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

(go
  (let [todo (<! c/todo)]
    (prn "todo recieved")
    (prn todo)))
