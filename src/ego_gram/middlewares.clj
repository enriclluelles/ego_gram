(ns ego-gram.middlewares
  (:require
    [compojure.core :refer :all]
    [ego-gram.data :as data]))


(def ^:dynamic *current-user* nil)

(defn current-user []
  @*current-user*)

(defn wrap-with-current-user [handler]
  (fn [request]
    (let [user-token ((request :headers) "x-session-token")
          user-from-token (future (data/find-user-by "access_token" user-token))]
      (binding [*current-user* user-from-token] (handler request)))))

(defn with-authentication [& routes]
  (fn [request]
    (if (= nil (current-user))
      {:status 401 :body "You need to authenticate first" :headers {}}
      (apply routing request routes))))

(defn wrap-cors-allow-all [handler]
  ; we define a function that adds the headers we need to a response
  (let [add-headers-to-response (fn [response request]
                                  (let [old-headers (:headers response)
                                        request-headers (:headers request)
                                        allowed-headers (request-headers "access-control-request-headers")
                                        to-add {"Access-Control-Allow-Origin" "*"
                                                "Access-Control-Allow-Headers" allowed-headers
                                                "Access-Control-Allow-Methods" "GET, POST, PUT, DELETE"}
                                        new-headers (conj old-headers to-add)]
                                    (assoc response :headers new-headers)))]
    (fn [request]
      ; depending on wether the request is an options or not we call the next middleware
      ; and add the headers to the response or just return a blank response with the headers
      (let [response (if (= :options (:request-method request))
                       {:status 200 :headers {} :body ""}
                       (handler request))]
        (add-headers-to-response response request)))))
