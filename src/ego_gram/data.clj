(ns ego-gram.data
  (:require
    [clojure.java.jdbc :as sql]
    [clojure.string :as str]
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
    [:user_id "varchar(255)" "references users(id)"]
    [:action "varchar(255)"]
    [:likes "integer"]
    [:target "varchar(255)"]
    [:updated_at "timestamp"]
    [:created_at "timestamp"]))

(def campaign-field-names
  (map first campaign-fields))

(def connection
  (System/getenv "DB_URL"))

(defn store-user [puser]
  (let [user (select-keys (clojure.walk/keywordize-keys puser) user-field-names)]
    (sql/insert! connection :users user)))

(defn all-from [table & [conditions & _]]
  (let [query-string (if (map? conditions)
                       (str "where " (str/join " and " (mapv (fn [[k v]]
                                                               (let [v (if (string? v)
                                                                         (str "'" v "'")
                                                                         v)]
                                                                 (str (name k) "=" v)))
                                                            conditions)))
                       (str conditions))]
    (sql/query connection (apply str ["select * from " table " " query-string]))))

(defn find-by [table field value]
  (let [query (apply str ["select * from " table " where " (name field) " = ? limit 1"])
        result (sql/query connection [query value])]
    (first result)))

(defn delete [table id]
  (sql/delete! connection (name table) ["id = ?" (Integer/parseInt (str id))]))

(defn update [table id data]
  (sql/update! connection (name table) data ["id = ?" (Integer/parseInt (str id))]))

(defn find-user-by [field value]
  (find-by "users" field value))

(defn find-campaign-by [field value]
  (find-by "campaigns" field value))


(defn create-user-table []
  (sql/db-do-commands connection
                      (apply sql/create-table-ddl
                             (concat '(:users) user-fields))))

(defn store-campaign [pcampaign]
  (let [campaign (select-keys (clojure.walk/keywordize-keys pcampaign) campaign-field-names)
        result-campaign (sql/insert! connection :campaigns campaign)]
    result-campaign))

(defn create-campaigns-table []
  (sql/db-do-commands connection
                      (apply sql/create-table-ddl
                             (concat '(:campaigns) campaign-fields))))
