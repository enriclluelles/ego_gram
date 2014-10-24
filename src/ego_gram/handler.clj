(ns ego-gram.handler
  (:require
   [ego-gram.instagram-client :as ig]
   [ego-gram.data :as data]
   [compojure.core :refer :all]
   [compojure.route :as route]
   [compojure.handler :as handler]
   [ring.middleware.json :as middleware]
   [ring.util.response :as response]))

(use '[ring.middleware.json :only [wrap-json-response]]
     '[ring.util.response :only [response]])

(defn authcb [code]
  (let [payload (ig/token-and-user-from-code code)]
    (if (= 200 (:code (:status payload)))
      (let [{tuser "user" taccess-token "access_token"} (:body payload)
            user (assoc tuser :access_token taccess-token)
            found-user (data/find-user-by :id (user "id"))]
        (if (not found-user)
          (data/store-user user))))))

(defroutes app-routes
  (GET "/" [] "")
  (GET "/auth" [] (response/redirect ig/auth-url))
  (GET "/authcb" [code] (authcb code) "Stored")
  (context "/api" []
           (context "/users" []
                    (GET "/" []
                         (ego-gram.data/all-users))
                    (GET "/:id" [id]
                         (list (ego-gram.data/find-user-by :id id))))
           (context "/campaigns" []
                    (GET "/" [] (all-campaigns))
                    (GET "/:id" [id]
                         (list (ego-gram.data/find-campaign-by :id (Integer/parseInt id))))
                    (POST "/" {params :params}
                          (ego-gram.data/store-campaign params))))
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (middleware/wrap-json-response (handler/api app-routes)))
