(ns ego-gram.worker
  (:require [ego-gram.data :as data]
            [ego-gram.instagram-client :as ig]
            ))

(defn perform-campaign
  [campaign]
  (let [action (campaign :action)
        target (campaign :target)
        user (data/find-by "users" "id" (campaign :user_id))
        user-token (user :access_token)]

    (case action
      ["likeHashtagPhotos"
       (doseq [media (ig/get-tagged-medias user-token {:tag_name target})]
           (Thread/sleep 500)
           (ig/post-like user-token {:media "id"}))
       ])))



(defn perform-all
  []
  (let [all-campaigns (data/all-from "campaigns")
        campaigns-grouped-by-user (group-by #(% :user_id) all-campaigns)]
    (doseq [[_ campaigns] campaigns-grouped-by-user]
      (future (doseq [campaign campaigns]
                (perform-campaign campaign))))))
