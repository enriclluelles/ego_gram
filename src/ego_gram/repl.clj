(ns ego-gram.repl
  (:require [ego-gram.handler :refer :all]
            [ring.adapter.jetty :as jetty]))

(defonce server (atom nil))

(defn start-server
  "used for starting the server in development mode from REPL"
  [& [port]]
  (let [port (Integer. (or port (System/getenv "PORT") 3000))]
    (reset! server (jetty/run-jetty #'app {:port port :join? false}))
    (println (str "You can view the site at http://localhost:" port))))

(defn -main
  []
  (start-server))

(defn stop-server []
  (.stop @server)
  (reset! server nil))
