(ns yardstick.process-channel
  (:require [clojure.core.async :refer [<! >! >!! go-loop]]
            [mount.core :as mount]
            [yardstick.channels :as c]
            [yardstick.dispatch-jobs :as dj]))

(>!! c/done "hello")

(defn- start-todo-processing [control ds]
  (go-loop [continue? @control]
    (let [job (<! c/todo)]
      (if continue?
        (do
          (dj/dispatch-job! job ds)
          (recur @control))
        (>! c/todo job)))))

(defn- start-done-processing [control ds]
  (go-loop [continue? @control]
    (let [result (<! c/done)]
      (if continue?
        (do
          (dj/process-done! result ds)
          (recur @control))
        (>! c/done result)))))

(defn- start-error-processing [control ds]
  (go-loop [continue? @control]
    (let [result (<! c/error)]
      (if continue?
        (do
          (dj/process-error! result ds)
          (recur @control))
        (>! c/error result)))))

(defn- start-processing [control ds]
  (start-todo-processing control ds)
  (start-done-processing control ds)
  (start-error-processing control ds))

(defn mount-channel-processing [ds]
  (let [control (atom true)]
    (mount/defstate channel-processing
      :start (start-processing control ds)
      :stop (reset! control false))))
