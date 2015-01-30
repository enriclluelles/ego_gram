(ns ego-gram.auth
  (:require [ego-gram.instagram-client :as ig]
            [ego-gram.data :as data]))

(defn authcb [code]
  "Provided with an authentication code from instagram, it returns
  a user if possible and stores it in the db"
  (let [payload (ig/token-and-user-from-code code)]
      (let [{tuser :user taccess-token :access_token} payload
            user (assoc tuser :access_token taccess-token)
            found-user (data/find-user-by :id (user :id))]
        (if (not found-user)
          (let [user-from-ig (ig/get-user {:user_id (user :id)})
                counts (get-in user-from-ig [:data :counts])]
            (data/store-user (assoc user :counts counts))))
        user)))
