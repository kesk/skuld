(ns skuld.data-model
  (:require [camel-snake-kebab.core :refer [->kebab-case ->snake_case]]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clojure.java.jdbc :as db]
            [skuld.common :refer [date-format]]
            [yesql.core :refer [defqueries]]
            [environ.core :refer [env]]))

(def db-spec {:classname "org.sqlite.JDBC"
              :subprotocol "sqlite"
              :subname (env :database-url)
              :foreign_keys "on"})

(def query-conf {:connection db-spec
                 :identifiers ->kebab-case})

(defqueries "skuld/queries.sql" query-conf)

(defn post-process [query-result]
  (transform-keys ->kebab-case query-result))

(defn insert!
  [table & rows]
  (map (keyword "last_insert_rowid()")
       (apply db/insert! db-spec table
              (conj (vec rows) :entities ->snake_case))))

; GROUPS
(defn create-group [group-name]
  (let [group-id (str (java.util.UUID/randomUUID))]
    (insert! :groups {:id group-id
                      :name group-name})
    group-id))

(defn get-group [group-id]
  (first (get-group-query {:id group-id} query-conf)))

(defn get-group-members [group-id]
  (get-group-members-query {:id group-id} query-conf))

; USERS
(defn create-user [username group-id]
  (first (insert! :users {:username username
                  :group-id group-id})))

(defn get-user [id]
  (first (get-user-query {:id id} query-conf)))

; EXPENSES
(defn create-expense
  ([payed-by amount]
   (create-expense payed-by amount (t/now)))
  ([payed-by amount date]
   (first (insert! :expenses {:payed-by payed-by
                              :amount amount
                              :date (str date)}))))

(defn get-expense [id]
  (let [expense (first (get-expense-query {:id id} query-conf))]
    (update expense :date #(f/parse date-format %))))

; DEPT
(defn create-dept [user-id expense-id group-id amount]
  (insert! :dept {:user-id user-id
                  :expense-id expense-id
                  :group-id group-id
                  :amount amount}))

(defn get-user-dept [user-id]
  (into {} (map (comp vec vals)
                (get-user-with-dept-query {:user_id user-id} query-conf))))

(defn get-dept [owed-by owed-to]
  (get (get-user-dept owed-by) owed-to))

(defn get-group-dept [group-id]
  (get-group-dept-query {:group_id group-id} query-conf))

(defn rebalance-dept
  [current-dept {:keys [owed-by owed-to amount]}]
  (if (nil? current-dept)
    {:amount amount
     :in-dept owed-by}
    (cond
      (= (:in-dept current-dept) owed-by) (update current-dept :amount + amount)
      (> (:amount current-dept) amount) (update current-dept :amount - amount)
      (< (:amount current-dept) amount) (-> current-dept
                                            (update :amount #(- amount %))
                                            (assoc :in-dept owed-by)))))

(defn calculate-dept [group-dept]
  (loop [group-dept group-dept
         calc-dept {}]
    (if (empty? group-dept) calc-dept
      (let [{:keys [owed-by owed-to amount] :as user-dept} (first group-dept)
            user-set #{owed-by owed-to}
            updated-dept (rebalance-dept (get calc-dept user-set) user-dept)]
        (if (nil? updated-dept)
          (recur (rest group-dept) (dissoc calc-dept user-set))
          (recur (rest group-dept) (assoc calc-dept user-set updated-dept)))))))

(defn add-shared-expense
  ([group-id paying-user-id amount]
   (add-shared-expense group-id paying-user-id amount
                       (map :id (get-group-members group-id))))
  ([group-id paying-user-id amount split-between]
   (let [expense-id (create-expense paying-user-id amount)
         user-dept (double (/ amount (count split-between)))]
     (doseq [dept-user-id (remove #{paying-user-id} split-between)]
       (create-dept dept-user-id expense-id group-id user-dept)))))
