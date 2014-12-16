(ns ego-gram.repl
  (:require [ego-gram.handler :refer :all]
            [ring.middleware.file-info :refer :all]
            [ring.adapter.jetty :as jetty]
            [environ.core :refer [env]]
            [ring.middleware.file :refer :all]))

(defonce server (atom nil))

(defn get-handler []
  ;; #'app expands to (var app) so that when we reload our code,
  ;; the server is forced to re-resolve the symbol in the var
  ;; rather than having its own copy. When the root binding
  ;; changes, the server picks it up without having to restart.
  (-> #'app
    ; Makes static assets in $PROJECT_DIR/resources/public/ available.
    (wrap-file "resources")
    ; Content-Type, Content-Length, and Last Modified headers for files in body
    (wrap-file-info)))

(defn start-server
  "used for starting the server in development mode from REPL"
  [& [port]]
  (let [port (Integer. (or port (env :port) 3000))]
    (reset! server (jetty/run-jetty (get-handler) {:port port :join? false}))
    (println (str "You can view the site at http://localhost:" port))))

(defn- main
  []
  (start-server))

(defn stop-server []
  (.stop @server)
  (reset! server nil))
