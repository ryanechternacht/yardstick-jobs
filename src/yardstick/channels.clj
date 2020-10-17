(ns yardstick.channels
  (:require [clojure.core.async :as async]))

(def max-jobs-in-system 10)
(def jobs-in-system (atom 0))

(def todo (async/chan max-jobs-in-system))
(def done (async/chan max-jobs-in-system))
(def error (async/chan max-jobs-in-system))
