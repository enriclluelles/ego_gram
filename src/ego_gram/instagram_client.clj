(ns ego-gram.instagram-client
  (:require
    [cheshire.core :as json]
    [ego-gram.middlewares :refer [current-user]]
    [instagram.oauth :refer :all]
    [instagram.callbacks :refer :all]
    [instagram.callbacks.handlers :refer :all]
    [instagram.api.endpoint :refer [authorization-url get-access-token]])
  (:import
    (instagram.callbacks.protocols SyncSingleCallback)))

(def instagram-credentials
  {:client-id (System/getenv "APP_ID")
   :client-secret (System/getenv "APP_SECRET")
   :redirect-uri "http://localhost:3000/auth_callback"})

(def auth-url (authorization-url instagram-credentials "likes comments relationships"))

(defn token-and-user-from-code [code]
  (get-access-token instagram-credentials code))

(defmacro ig-with-token [& funcs]
  `(do ~@(map (fn [c]
                `(defn ~(symbol c)
                   ([] (~(symbol c) {}))
                   ([params#] (~(symbol c) ((current-user) :access_token) params#))
                   ([access-token# params#]
                    (let [to-call# ~(symbol (str "instagram.api.endpoint/" c))
                          result# (to-call# :access-token access-token# :params params#)]
                      ((result# :body) "data"))))) funcs)))

(ig-with-token
  get-user get-popular get-current-user-liked-medias get-tagged-medias
  post-like)
