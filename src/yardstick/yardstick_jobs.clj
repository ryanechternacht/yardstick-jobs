(ns yardstick.yardstick-jobs
  (:gen-class)
  (:require [mount.core :as mount]
            [next.jdbc :as jdbc]
            [yardstick.process-channel :as pc]
            [yardstick.fetch-jobs :as fj]))

(def db-conn {:dbtype "mysql"
              :dbname "yardstick"
              :user "root"
              :password "root"
              :host "127.0.0.1"
              :port 8889})

(def ds (jdbc/get-datasource db-conn))

(defn setup-components [ds]
  (fj/mount-job-fetching ds)
  (pc/mount-channel-processing ds))

(comment
  (setup-components ds)
  (mount/start)
  (mount/stop))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (setup-components ds)
  (println "components setup")
  (mount/start)
  (println "components started"))