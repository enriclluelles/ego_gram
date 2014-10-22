(ns ego-gram.handler
  (:require
   [ego-gram.instagram-client :as ig]
   [ego-gram.data :as data]
   [compojure.core :refer :all]
   [compojure.handler :as handler]
   [ring.util.response :as response]
   [compojure.route :as route]))

(defn authcb [code]
  (let [payload (ig/token-and-user-from-code code)]
    (if (= 200 (:code (:status payload)))
      (let [{tuser "user" taccess-token "access_token"} (:body payload)
            user (assoc tuser :access_token taccess-token)]
        (println payload)
        (if (not (data/find-user-by :id (:id user)))
          (data/store-user user))))))

(defroutes app-routes
  (GET "/" [] {:body (ig/most-popular)
               :status 200
               :headers {"Content-Type" "application/json; ; charset=utf-8"}})
  (GET "/auth" [] (response/redirect ig/auth-url))
  (GET "/authcb" [code] (authcb code) "Stored")
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))
