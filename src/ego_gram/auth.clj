(ns ego-gram.auth
  (:require [ego-gram.instagram-client :as ig]
            [ego-gram.data :as data]))

(defn authcb [code]
  "Provided with an authentication code from instagram, it returns
  a user if possible and stores it in the db"
  (let [payload (ig/token-and-user-from-code code)]
    (if (= 200 (:code (:status payload)))
      (let [{tuser "user" taccess-token "access_token"} (:body payload)
            user (assoc tuser :access_token taccess-token)
            user-id (user "id")
            found-user (data/find-user-by :id user-id)
            user-from-instagram (ig/get-user {:user_id user-id})
            extended-user (merge user (user-from-instagram "counts"))]
        (if (not found-user)
            (data/store-user extended-user))
        extended-user))))
