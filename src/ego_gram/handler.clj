(ns ego-gram.handler
  (:require
    [ego-gram.auth :refer :all]
    [ego-gram.instagram-client :as ig]
    [ego-gram.data :as data]
    [ego-gram.worker :as worker]
    [ego-gram.campaigns :as campaigns]
    [ego-gram.middlewares :refer :all]
    [compojure.core :refer :all]
    [compojure.route :as route]
    [compojure.handler]
    [ring.middleware.cors]
    [ring.middleware.json]
    [ring.util.response :refer :all]))

(defn all-users []
  {:body {:users (data/all-from "users")}})

(defn find-user [id]
  {:body {:users (list (data/find-by "users" :id id))}})

(defn media [liked popular]
  (let [body (if liked
               ((ig/get-current-user-liked-medias) "data")
               (if popular
                 ((ig/get-popular) "data")))]
    {:body {:medias body}}))

(defn show-session []
  (let [user-from-store (current-user)
        user-id (user-from-store :id)
        access-token (user-from-store :access_token)
        info-from-instagram ((ig/get-user {:user_id user-id}) "data")
        counts-from-instagram (info-from-instagram "counts")
        initial-counts (apply hash-map (interleave [:following :followers :media] (map user-from-store [:follows :followed_by :media])))
        current-counts (apply hash-map (interleave [:following :followers :media] (map counts-from-instagram ["follows" "followed_by" "media"])))
        without-counts (dissoc user-from-store :follows :followed_by :media)
        user (assoc without-counts :initial_counts initial-counts :current_counts current-counts)]
    {:body {:user user}}))

(defroutes app-routes
  (GET "/" [] "")
  (GET "/work!" [] (worker/perform-all-with-timer) "")
  (GET "/auth" [] (redirect ig/auth-url))
  (GET "/auth_callback" [code]
       (let [frontend-url (System/getenv "FRONTEND_CALLBACK_URL")
             token ((authcb code) :access_token)
             url-with-params (str frontend-url "/dashboard?session_token=" token)]
         (redirect url-with-params)))
  (context "/api" []
           (with-authentication
             (context "/users" []
                      (GET "/" [] (all-users))
                      (GET "/:id" [id] (find-user id)))
             (GET "/media" {{liked :liked popular :popular} :params}
                  (media liked popular))
             (context "/campaigns" []
                      (GET "/" []
                           (campaigns/all))
                      (GET "/:id" [id]
                           (campaigns/find id))
                      (DELETE "/:id" [id]
                              (campaigns/delete id))
                      (PUT "/:id" {{id :id campaign :campaign} :params}
                           (campaigns/update id campaign))
                      (POST "/" {{campaign :campaign} :params}
                            (campaigns/create campaign)))
             (context "/session" []
                      (GET "/" [] (show-session)))))
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (-> app-routes
      compojure.handler/api
      ring.middleware.json/wrap-json-response
      ring.middleware.json/wrap-json-params
      wrap-with-current-user
      (ring.middleware.cors/wrap-cors :access-control-allow-headers :any
                                      :access-control-allow-origin #".*"
                                      :access-control-allow-methods [:get :post :put :delete])))
