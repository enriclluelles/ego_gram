(ns ego-gram.handler
  (:require
    [ego-gram.instagram-client :as ig]
    [ego-gram.data :as data]
    [compojure.core :refer :all]
    [compojure.route :as route]
    [compojure.handler :as handler]
    [ring.middleware.json :as m]
    [ring.util.response :refer :all]
    [noir.util.middleware :as nm]
    [noir.session :as session]
    [ring.util.response :as response]))

(defn authcb [code]
  (let [payload (ig/token-and-user-from-code code)]
    (if (= 200 (:code (:status payload)))
      (let [{tuser "user" taccess-token "access_token"} (:body payload)
            user (assoc tuser :access_token taccess-token)
            user-id (user "id")
            found-user (data/find-user-by :id user-id)
            user-from-instagram (ig/get-user-info taccess-token user-id)
            extended-user (merge user (user-from-instagram "counts"))]
        (session/put! :user extended-user)
        (if found-user
          (session/put! :user found-user)
          (do
            (data/store-user extended-user)
            (session/put! :user extended-user)))))))

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

(defn show-session []
  (let [user-from-store (session/get :user)
        user-id (user-from-store :id)
        access-token (user-from-store :access_token)
        info-from-instagram (ig/get-user-info access-token user-id)
        counts-from-instagram (info-from-instagram "counts")
        initial-counts (apply hash-map (interleave [:following :followers :media] (map user-from-store [:follows :followed_by :media])))
        current-counts (apply hash-map (interleave [:following :followers :media] (map counts-from-instagram ["follows" "followed_by" "media"])))
        without-counts (dissoc user-from-store :follows :followed_by :media)
        user (assoc without-counts :initial_counts initial-counts :current_counts current-counts)]
    {:body {:user user}}))

(defroutes app-routes
  (GET "/" [] "")
  (GET "/auth" [] (response/redirect ig/auth-url))
  (GET "/auth_callback" [code] (authcb code) (redirect "http://google.com"))
  (context "/api" []
           (context "/users" []
                    (GET "/" [] (all-users))
                    (GET "/:id" [id] (find-user id)))
           (context "/campaigns" []
                    (GET "/" [] (all-campaigns))
                    (GET "/:id" [id] (find-campaign))
                    (POST "/" {params :params}
                          (ego-gram.data/store-campaign params)))
           (context "/session" []
                    (GET "/" [] (show-session))))
  (route/resources "/")
  (route/not-found "Not Found"))

  (def app
    (nm/app-handler [(m/wrap-json-response (handler/api app-routes))]))
