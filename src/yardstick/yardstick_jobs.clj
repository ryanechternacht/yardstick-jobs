(ns yardstick.yardstick-jobs
  (:gen-class)
  (:require [mount.core :as mount]
            [next.jdbc :as jdbc]
            [yardstick.process-channel :as pc]
            [yardstick.fetch-jobs :as fj]))

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

(fj/mount-job-fetching ds)

(pc/mount-channel-processing ds)

(mount/start)

(mount/stop)
