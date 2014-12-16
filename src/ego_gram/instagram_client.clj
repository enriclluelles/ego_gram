(ns ego-gram.instagram-client
  (:require
    [ego-gram.middlewares :refer [current-user]]
    [instagram.oauth :refer :all]
    [instagram.callbacks :refer :all]
    [instagram.callbacks.handlers :refer :all]
    [instagram.api.endpoint :refer [authorization-url get-access-token]])
  (:import
    (instagram.callbacks.protocols SyncSingleCallback)))

(defonce credentials (atom nil))

(defn instagram-credentials [& [host & _]]
  (swap! credentials #(if %1 %1 %2) {:client-id (System/getenv "APP_ID")
                                     :client-secret (System/getenv "APP_SECRET")
                                     :redirect-uri (str "http://" host "/auth_callback")}))

(defn auth-url [& [host & _]] (authorization-url (instagram-credentials host) "likes comments relationships"))

(defn token-and-user-from-code [code]
  (get-access-token (instagram-credentials) code))

(defmacro ig-with-token [& funcs]
  `(do ~@(map (fn [c]
                `(defn ~(symbol c)
                   ([] (~(symbol c) {}))
                   ([params#] (~(symbol c) ((current-user) :access_token) params#))
                   ([access-token# params#]
                    (let [to-call# ~(symbol (str "instagram.api.endpoint/" c))]
                      ((to-call# :access-token access-token# :params params#) :body)))))
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
