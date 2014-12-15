(ns ego-gram.campaigns
  (:refer-clojure :exclude [find])
  (:require
    [ego-gram.middlewares :refer [current-user]]
    [ego-gram.data :as data]))

(defn all []
  {:body {:campaigns (data/all-from "campaigns" {:user_id ((current-user) :id)})}})

(defn find [id]
  (let [int-id (Integer/parseInt id)
        body (list (data/find-by "campaigns" :id int-id))]
    {:body {:campaigns body}}))

(defn delete [id]
  (data/delete "campaigns" id)
  {:status 204})

(defn update [id campaign]
  (let [id (Integer/parseInt id)
        update-result (first (data/update "campaigns" id campaign))]
    (if (> update-result 0)
      {:body (data/find-by "campaigns" "id" id)}
      {:status 404})))

(defn create [campaign]
  (let [campaign (assoc campaign :user_id ((current-user) :id))]
  {:body {:campaigns (data/store-campaign campaign)}}))
