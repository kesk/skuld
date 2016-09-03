(ns skuld.data-model
  (:require [camel-snake-kebab.core :refer [->kebab-case ->snake_case]]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clojure.java.jdbc :as db]
            [clojure.tools.logging :as log]
            [environ.core :refer [env]]
            [skuld.common :refer [date-format]]
            [yesql.core :refer [defqueries]]))

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

; USERS
(defn create-user [username group-id]
  (first (insert! :users {:name username
                          :group-id group-id})))

; GROUPS
(defn create-group [group-name users]
  (if (empty? users)
    nil
    (let [group-id (str (java.util.UUID/randomUUID))]
      (insert! :groups {:id group-id
                        :name group-name})
      (doseq [username (set users)] (create-user username group-id))
      group-id)))

(defn get-group [group-id]
  (let [result (get-group-query {:group_id group-id} query-conf)]
    (if (empty? result)
      nil
      {:id group-id
       :name (:group-name (first result))
       :members (vec (map :name result))})))

(defn get-group-expenses [group-id]
  (let [result (get-group-expenses-query {:group_id group-id} query-conf)]
    result))

; EXPENSES
(defn create-expense
  ([group-id payed-by amount]
   (create-expense group-id payed-by amount (t/now)))
  ([group-id payed-by amount date]
   (first (insert! :expenses {:payed-by payed-by
                              :group-id group-id
                              :amount amount
                              :date (str date)}))))

(defn get-expense [id]
  (let [expense (first (get-expense-query {:id id} query-conf))]
    (update expense :date #(f/parse date-format %))))

; DEPT
(defn create-dept [user-name expense-id group-id amount]
  (insert! :dept {:user-name user-name
                  :expense-id expense-id
                  :group-id group-id
                  :amount amount}))

(defn get-user-dept [user-name group-id]
  (into {} (map (comp vec vals)
                (get-user-with-dept-query {:user_name user-name
                                           :group_id group-id}
                                          query-conf))))

(defn get-dept [group-id owed-by owed-to]
  (get (get-user-dept owed-by group-id) owed-to))

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
  ([group-id paying-user-name amount]
   (add-shared-expense group-id paying-user-name amount
                       (:members (get-group group-id))))
  ([group-id paying-user-name amount split-between]
   (let [expense-id (create-expense group-id paying-user-name amount)
         user-dept (double (/ amount (count split-between)))]
     (doseq [dept-user-name (remove #{paying-user-name} split-between)]
       (create-dept dept-user-name expense-id group-id user-dept)))))
