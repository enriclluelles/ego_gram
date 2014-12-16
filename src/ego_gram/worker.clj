(ns ego-gram.worker
  (:require [ego-gram.data :as data]
            [ego-gram.instagram-client :as ig]))

(defn perform-campaign
  [campaign]
  (let [action (campaign :action)
        target (campaign :target)
        likes (campaign :likes)
        user (data/find-by "users" "id" (campaign :user_id))
        user-token (user :access_token)]

    (case action
      ["likeHashtagPhotos" (ig/like-medias-from-tag user-token target likes)])))


(defn perform-all
  []
  (let [all-campaigns (data/all-from "campaigns")
        campaigns-grouped-by-user (group-by #(% :user_id) all-campaigns)]
    (doseq [[_ campaigns] campaigns-grouped-by-user]
      (future (doseq [campaign campaigns]
          (println "starting campaign work: " (campaign :id))
          (perform-campaign campaign))))))

(def can-start? (atom 0))

(defn perform-all-with-timer
  []
  (if (= (swap! can-start? inc) 1)
    (while true
      (println "Working!")
      (future (perform-all))
      (Thread/sleep (* 60 60 1000)))))
