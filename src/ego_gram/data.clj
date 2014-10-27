(ns ego-gram.data
  (:require
    [clojure.java.jdbc :as sql]
    [clojure.walk]))

(def user-fields
  '([:id "varchar(255)" :primary :key]
    [:username "varchar(255)"]
    [:website "varchar(255)"]
    [:profile_picture "varchar(255)"]
    [:full_name "varchar(255)"]
    [:bio "varchar(255)"]
    [:followed_by "integer"]
    [:follows "integer"]
    [:media "integer"]
    [:access_token "varchar(255)"]))

(def user-field-names
  (map first user-fields))

(def campaign-fields
  '([:id "serial" :primary :key]
    [:action "varchar(255)"]
    [:target "varchar(255)"]))

(def campaign-field-names
  (map first campaign-fields))

(def connection
  (System/getenv "DB_URL"))

(defn store-user [puser]
  (let [user (select-keys (clojure.walk/keywordize-keys puser) user-field-names)]
    (sql/insert! connection :users user)))

(defn all-from-table [table]
  (sql/query connection (apply str ["select * from " table])))

(defn all-users []
  (all-from-table "users"))

(defn all-campaigns []
  (all-from-table "campaigns"))

(defn find-by [table field value]
  (let [query (apply str ["select * from " table " where " (name field) " = ? limit 1"])
        result (sql/query connection [query value])]
    (first result)))


(defn find-user-by [field value]
  (find-by "users" field value))

(defn find-campaign-by [field value]
  (find-by "campaigns" field value))


(defn create-user-table []
  (sql/db-do-commands connection
                      (apply sql/create-table-ddl
                             (concat '(:users) user-fields))))

(defn store-campaign [pcampaign]
  (let [campaign (select-keys (clojure.walk/keywordize-keys pcampaign) campaign-field-names)]
    (sql/insert! connection :campaigns campaign)))

(defn create-campaigns-table []
  (sql/db-do-commands connection
                      (apply sql/create-table-ddl
                             (concat '(:campaigns) campaign-fields))))
