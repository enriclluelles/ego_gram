(defproject ego-gram-clojure "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [compojure "1.1.9"]
                 [instagram-api "0.1.7"]
                 [ring/ring-json "0.3.1"]
                 [org.clojure/java.jdbc "0.3.5"]
                 [postgresql "9.3-1102.jdbc4"]
                 [lib-noir "0.9.4"]
                 [ring/ring-jetty-adapter "1.2.2"]
                 [cheshire "5.3.1"]]
  :plugins [[lein-ring "0.8.12"]]
  :ring {:handler ego-gram.handler/app
         :port 3000}
  :profiles {:dev
             {:dependencies [[javax.servlet/servlet-api "2.5"]
                             [ring-mock "0.1.5"]]}})
