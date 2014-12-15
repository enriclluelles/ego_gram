(ns ring.middleware.cors
  "Ring middleware for Cross-Origin Resource Sharing."
  (:require [clojure.string :as str]
            [clojure.set :as set]))

(defn origin
  "Returns the Origin request header."
  [request] (get (:headers request) "origin"))

(defn preflight?
  "Returns true if the request is a preflight request"
  [request]
  (= (request :request-method) :options))

(defn string-to-set
  "Transforms a comma-separated string to a set"
  [the-string]
  (set (map str/trim (str/split the-string #","))))

(defn allow-preflight-headers?
  "Returns true if the request is a preflight request and all the headers that
  it's going to use are allowed. Returns false otherwise."
  [request allowed-headers]
  (if (= allowed-headers :any)
    true
    (let [request-headers (get-in request [:headers "access-control-request-headers"] "")
          request-headers-set (string-to-set request-headers)
          allowed-headers-set (set (map name allowed-headers))]
      (= (count allowed-headers)
         (count (clojure.set/intersection allowed-headers-set request-headers-set))))))

(defn allow-method?
  "In the case of regular requests it checks if the request-method is allowed.
  In the case of preflight requests it checks if the access-control-request-method is allowed."
  [request allowed-methods]
  (contains? allowed-methods (if (preflight? request)
                               (keyword (str/lower-case
                                          (get-in request [:headers "access-control-request-method"] "")))
                               (:request-method request))))

(defn allow-request?
  "Returns true if the request's origin matches the access control
  origin, otherwise false."
  [request access-control]
  (let [origin (origin request)
        allowed-origins (:access-control-allow-origin access-control)
        allowed-headers (:access-control-allow-headers access-control)
        allowed-methods (:access-control-allow-methods access-control)]
    (if (and origin
             (seq allowed-origins)
             (seq allowed-methods)
             (some #(re-matches % origin) allowed-origins)
             (if (preflight? request)
               (allow-preflight-headers? request allowed-headers)
               true)
             (allow-method? request allowed-methods))
      true false)))

(defn header-name
  "Returns the capitalized header name as a string."
  [header]
  (if header
    (->> (str/split (name header) #"-")
         (map str/capitalize )
         (str/join "-" ))))

(defn normalize-headers
  "Normalize the headers by converting them to capitalized strings."
  [headers]
  (reduce
    (fn [acc [k v]]
      (assoc acc (header-name k)
             (case k
               :access-control-allow-methods (str/join ", " (sort (map (comp str/upper-case name) v)))
               :access-control-allow-headers (str/join ", " (sort (map (comp header-name name) v)))
               v)))
    {} headers))

(defn add-origin
  "Add the access control headers using the request's origin to the response."
  [request access-control response]
  (if-let [origin (origin request)]
    (update-in response [:headers] merge
               {:access-control-allow-origin origin
                :access-control-allow-methods (access-control :access-control-allow-methods)})
    response))

(defn add-allowed-headers
  "Adds the allowed headers to the request"
  [request access-control response]
  (if (preflight? request)
    (let [request-headers (get-in request [:headers "access-control-request-headers"])
          access-control-allowed-headers (access-control :access-control-allow-headers)
          allowed-headers (if (= access-control-allowed-headers :any)
                            (string-to-set request-headers)
                            access-control-allowed-headers)]
      (if allowed-headers
        (update-in response [:headers] merge {:access-control-allow-headers allowed-headers})
        response))
    response))


(defn add-access-control
  "Add the access-control headers to the response based on the rules and what came on the
  header."
  [request access-control response]
  (let [unnormalized-response (->> response
                                   (add-origin request access-control)
                                   (add-allowed-headers request access-control))]
    (update-in unnormalized-response [:headers] normalize-headers)))

(defn normalize-config
  [access-control]
  (-> (apply hash-map access-control)
      (update-in [:access-control-allow-methods] set)
      (update-in [:access-control-allow-headers] #(if (coll? %) (set %) %))
      (update-in [:access-control-allow-origin] #(if (sequential? %) % [%]))))

(defn wrap-cors
  "Middleware that adds Cross-Origin Resource Sharing headers.

  (def handler
    (-> routes
        (wrap-cors
         :access-control-allow-origin #\"http://example.com\"
         :access-control-allow-methods [:get :put :post :delete])))
"
  [handler & access-control]
  (let [access-control (normalize-config access-control)]
    (fn [request]
      (if (and (preflight? request) (allow-request? request access-control))
        (let [blank-response {:status 200 :headers {} :body "preflight complete"}]
          (add-access-control request access-control blank-response))
        (if (origin request)
          (if (allow-request? request access-control)
            (let [response (handler request)]
              (add-access-control request access-control response)))
          (handler request))))))
