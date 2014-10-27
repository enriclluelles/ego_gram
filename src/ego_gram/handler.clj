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
            user-id (user "id")
            found-user (data/find-user-by :id user-id)
            user-from-instagram (ig/get-user-info taccess-token user-id)
            extended-user (merge user (user-from-instagram "counts"))]
        (if (not found-user)
          (data/store-user extended-user))))))

(defn all-users []
  {:body {:users (ego-gram.data/all-users)}})

(defn find-user [id]
  {:body {:users (list (ego-gram.data/find-user-by :id id))}})

(defn all-campaigns []
  {:body {:campaigns (ego-gram.data/all-campaigns)}})

(defn find-campaign [id]
  (let [intId (Integer/parseInt id)
        body (list (ego-gram.data/find-campaign-by :id intId))]
    {:body {:campaigns body}}))


(defroutes app-routes
  (GET "/" [] "")
  (GET "/auth" [] (response/redirect ig/auth-url))
  (GET "/auth_callback" [code] (authcb code) "Stored")
  (context "/api" []
           (context "/users" []
                    (GET "/" [] (all-users))
                    (GET "/:id" [id] (find-user id)))
           (context "/campaigns" []
                    (GET "/" [] (all-campaigns))
                    (GET "/:id" [id] (find-campaign))
                    (POST "/" {params :params}
                          (ego-gram.data/store-campaign params))))
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (middleware/wrap-json-response (handler/api app-routes)))
