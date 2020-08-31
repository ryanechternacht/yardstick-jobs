(ns yardstick.yardstick-jobs
  (:gen-class)
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as jdbc-sql]
            [honeysql.core :as sql]
            [honeysql.helpers :refer [select from merge-where insert-into values]]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [yardstick.jobs.sample-students :as ss]
            [yardstick.process-job :as pj]))

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

; (def db-call {:select [:*]
;               :from [:student]})
(def db-call (-> (select :*)
                 (from :student)
                 (merge-where [:= :id 4])))

(sql/format db-call)

(with-open [conn (jdbc/get-connection ds)]
  (jdbc/execute! conn (sql/format db-call)))

(defn parse-line
  [line-vec]
  (let [[first-name last-name local-id state-id gender] line-vec]
    {:first-name first-name
     :last-name last-name
     :local-id local-id
     :state-id state-id
     :gender gender}))

(with-open [reader (io/reader "resources/sample-students.csv")]
  (doall
   (->> (csv/read-csv reader)
        (drop 1)
        (map parse-line))))

(with-open [conn (jdbc/get-connection ds)]
  (jdbc-sql/insert! conn :student
                    {:tenant-id 1 :first-name "ryan", :last-name "echternacht", :local-id "123", :state-id "oh123", :gender "m"}))

(def insert-call (-> (insert-into :student)
                     (values [{:tenant-id 1 :first-name "ryan", :last-name "echternacht", :local-id "123", :state-id "oh123", :gender "m"}
                              {:tenant-id 1 :first-name "mason", :last-name "cook", :local-id "456", :state-id "oh456", :gender "m"}
                              {:tenant-id 1 :first-name "grace", :last-name "ooi", :local-id "789", :state-id "oh789", :gender "f"}])))

(sql/format insert-call)

(with-open [conn (jdbc/get-connection ds)]
  (jdbc/execute! conn (sql/format insert-call)))

(def f (pj/build-parser (:columns ss/job)))

(def col ["ryan" "echternacht" "123" "oh123" "m"])

(f col)
