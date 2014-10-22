(ns ego-gram.data
  (:require
    [clojure.java.jdbc :as sql]))

(def connection
  (System/getenv "DB_URL"))

(defn store-user [user]
  (sql/insert! connection :users user))

(defn find-user-by [field value]
  (let [query (apply str ["select * from users where " (name field) " = ? limit 1"])]
  (sql/query connection [query value])))

(defn create-user-table []
  (sql/db-do-commands connection
                      (sql/create-table-ddl :users
                                            [:id "varchar(255)" :primary :key]
                                            [:username "varchar(255)"]
                                            [:website "varchar(255)"]
                                            [:profile_picture "varchar(255)"]
                                            [:full_name "varchar(255)"]
                                            [:bio "varchar(255)"]
                                            [:access_token "varchar(255)"])))
