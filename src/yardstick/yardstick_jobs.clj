(ns yardstick.yardstick-jobs
  (:gen-class)
  (:require [next.jdbc :as jdbc]
            [honeysql.core :as sql]
            [honeysql.helpers :refer [select from merge-where]]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]))

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
                 (merge-where [:= :id 3])))

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

