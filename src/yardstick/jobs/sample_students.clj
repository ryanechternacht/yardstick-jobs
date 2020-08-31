(ns yardstick.jobs.sample-students
  ; (:require [])
  )

(def ^:private columns
  [{:name :first-name
    :csv-column 0 ; TODO make this an array
    :parse identity} ; TODO make this accept rest
   {:name :last-name
    :csv-column 1
    :parse identity}
   {:name :local-id
    :csv-column 2
    :parse identity}
   {:name :state-id
    :csv-column 3
    :parse identity}
   {:name :gender
    :csv-column 4
    :parse identity}
   {:name :age
    :csv-column 5
    :parse #(Integer/parseInt %)}])

(def job
  {:columns columns})