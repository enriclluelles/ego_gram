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
        (if (not found-user)
            (data/store-user extended-user))
        extended-user))))

(defn all-users []
  {:body {:users (ego-gram.data/all-users)}})

(defn find-user [id]
  {:body {:users (list (ego-gram.data/find-user-by :id id))}})

(defn all-campaigns []
  {:body {:campaigns (ego-gram.data/all-campaigns)}})

(defn find-campaign [id]
  (let [int-id (Integer/parseInt id)
        body (list (ego-gram.data/find-campaign-by :id int-id))]
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
  (GET "/" [] "shit")
  (GET "/auth" [] (response/redirect ig/auth-url))
  (GET "/auth_callback" [code]
       (let [frontend-url (System/getenv "FRONTEND_CALLBACK_URL")
             token ((authcb code) :access_token)
             url-with-params (str frontend-url "/dashboard?session_token=" token)]
         (redirect url-with-params)))
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

(defn wrap-cors-allow-all [handler]
  ; we define a function that adds the headers we need to a response
  (let [add-headers-to-response (fn [response]
                                  (let [old-headers (:headers response)
                                        to-add '("Access-Control-Allow-Origin" "*" "Access-Control-Allow-Headers" "X-Session-Token")
                                        new-headers (apply assoc old-headers to-add)]
                                    (assoc response :headers new-headers)))]
    (fn [request]
      ; depending on wether the request is an options or not we call the next middleware
      ; and add the headers to the response or just return a blank response with the headers
      (let [response (if (= :options (:request-method request))
                       {:status 200 :headers {} :body ""}
                       (handler request))]
        (add-headers-to-response response)))))

(def app
  (wrap-cors-allow-all (nm/app-handler [(m/wrap-json-response (handler/api app-routes))])))
