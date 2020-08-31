(ns yardstick.jobs.sample-students
  ; (:require [])
  )

(def ^:private attributes
  [{:name :student/first-name
    :csv-column 0 ; TODO make this an array
    :parse identity} ; TODO make this accept rest
   {:name :student/last-name
    :csv-column 1
    :parse identity}
   {:name :student/local-id
    :csv-column 2
    :parse identity}
   {:name :student/state-id
    :csv-column 3
    :parse identity}
   {:name :student/gender
    :csv-column 4
    :parse identity}
   {:name :student/age
    :csv-column 5
    :parse #(Integer/parseInt %)}])

(def job
  {:attributes attributes
   :db-table :student})