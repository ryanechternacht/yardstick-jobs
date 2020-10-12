(ns yardstick.jobs.sample-students)

(def ^:private attributes
  [{:col-name :first-name
    :csv-column 0 ; TODO make this an array
    :parse identity ; TODO make this accept rest
    :spec string?}
   {:col-name :last-name
    :csv-column 1
    :parse identity
    :spec string?}
   {:col-name :local-id
    :csv-column 2
    :parse identity
    :spec string?}
   {:col-name :state-id
    :csv-column 3
    :parse identity
    :spec string?}
   {:col-name :gender
    :csv-column 4
    :parse #(String/.toUpperCase %)
    :spec #{"M" "F"}}
   {:col-name :age
    :csv-column 5
    :parse #(Integer/parseInt %)
    :spec int?}])

(def job
  {:attributes attributes
   :db-table :student})