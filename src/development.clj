(ns user
 (:require ego-gram.handler)
 (:use ring.util.serve))

(defn run-server []
  (serve ego-gram.handler/app))
