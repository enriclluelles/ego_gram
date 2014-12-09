(ns ego-gram.handler
  (:require
    [ego-gram.auth :refer :all]
    [ego-gram.instagram-client :as ig]
    [ego-gram.middlewares :refer :all]
    [compojure.core :refer :all]
    [compojure.route :as route]
    [compojure.handler]
    [ring.middleware.json]
    [ring.util.response :refer :all]))

(defn all-users []
  {:body {:users (ego-gram.data/all-users)}})

(defn find-user [id]
  {:body {:users (list (ego-gram.data/find-user-by :id id))}})

(defn all-campaigns []
  {:body {:campaigns (ego-gram.data/all-campaigns)}})

(defn media [liked popular]
  (let [body (if liked
               (ig/get-current-user-liked-medias)
               (if popular
                 (ig/get-popular)))]
    {:body body}))

(defn find-campaign [id]
  (let [int-id (Integer/parseInt id)
        body (list (ego-gram.data/find-campaign-by :id int-id))]
    {:body {:campaigns body}}))

(defn show-session []
  (let [user-from-store (current-user)
        user-id (user-from-store :id)
        access-token (user-from-store :access_token)
        info-from-instagram (ig/get-user {:user_id user-id})
        counts-from-instagram (info-from-instagram "counts")
        initial-counts (apply hash-map (interleave [:following :followers :media] (map user-from-store [:follows :followed_by :media])))
        current-counts (apply hash-map (interleave [:following :followers :media] (map counts-from-instagram ["follows" "followed_by" "media"])))
        without-counts (dissoc user-from-store :follows :followed_by :media)
        user (assoc without-counts :initial_counts initial-counts :current_counts current-counts)]
    {:body {:user user}}))

(defroutes app-routes
  (GET "/" [] "")
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
                      (GET "/" [] (all-campaigns))
                      (GET "/:id" [id] (find-campaign))
                      (POST "/" {params :params}
                            (ego-gram.data/store-campaign params)))
             (context "/session" []
                      (GET "/" [] (show-session)))))
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (-> app-routes
      compojure.handler/api
      ring.middleware.json/wrap-json-response
      wrap-with-current-user
      wrap-cors-allow-all))
