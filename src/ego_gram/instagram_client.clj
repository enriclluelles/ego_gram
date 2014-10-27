(ns ego-gram.instagram-client
  (:require
    [cheshire.core :as json])
  (:use
    instagram.oauth
    instagram.callbacks
    instagram.callbacks.handlers
    instagram.api.endpoint)
  (:import
    (instagram.callbacks.protocols SyncSingleCallback)))

(def instagram-credentials
  {:client-id (System/getenv "APP_ID")
   :client-secret (System/getenv "APP_SECRET")
   :redirect-uri "http://localhost:3000/auth_callback"})

(defn most-popular []
  (let [payload (get-popular :oauth instagram-credentials)
        body (:body payload)
        status (:status payload)]
    (json/generate-string body)))

(def auth-url (authorization-url instagram-credentials "likes comments relationships"))

(defn token-and-user-from-code [code]
  (get-access-token instagram-credentials code))

(defn get-user-info [access-token id]
  (let [payload-from-instagram (get-user :access-token access-token :params {:user_id id})]
    ((payload-from-instagram :body) "data")))
