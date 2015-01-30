(ns ego-gram.instagram-client
  (:require
    [ego-gram.middlewares :refer [current-user]]
    [clj-http.client]
    [clojuregram.core :as clojuregram]))

(def instagram-credentials
  {:client_id (System/getenv "APP_ID")
   :client_secret (System/getenv "APP_SECRET")
   :client_ips (clojure.string/trim-newline
                 ((clj-http.client/get "http://checkip.amazonaws.com") :body))
   :scope "likes comments relationships"
   :redirect-uri (str (System/getenv "AUTH_HOST") "/auth_callback")})

(def auth-url (clojuregram/access-token-url instagram-credentials))

(defn token-and-user-from-code [code]
  (clojuregram/get-access-token instagram-credentials code))

(defmacro ig-with-token [& funcs]
  `(do ~@(map (fn [c]
                `(defn ~(symbol c)
                   ([] (~(symbol c) {}))
                   ([params#] (~(symbol c) ((current-user) :access_token) params#))
                   ([access-token# params#]
                    (clojuregram/with-access-token-and-credentials access-token# instagram-credentials
                      (let [to-call# ~(symbol (str "clojuregram.core/" c))
                            result# ((to-call# params#) :body)]
                        result#)))))
              funcs)))

(ig-with-token
  get-user get-popular get-current-user-liked-medias get-tagged-medias
  post-like)

(defn like-medias-from-tag
  [access-token tag how-many]
  (let [media-to-like (loop [media [] pager nil]
                        (Thread/sleep 500)
                        (let [response (get-tagged-medias access-token {:tag_name tag, :max_tag_id pager})
                              response-media (response "data")
                              next_page (get-in response ["pagination" "next_max_tag_id"])
                              media (vec (concat response-media media))]
                          (if (>= (count media) how-many)
                            media
                            (recur media next_page))))]

    (doseq [element media-to-like]
      (Thread/sleep 1000)
      (prn "liking: " (element "id"))
      (prn (post-like access-token {:media_id (element "id")})))))
