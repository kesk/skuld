(ns skuld.data-model
  (:require [camel-snake-kebab.core :refer [->kebab-case ->snake_case]]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clojure.java.jdbc :as db]
            [environ.core :refer [env]]
            [skuld.common :refer [date-format]]
            [yesql.core :refer [defqueries]]))

(def db-spec {:classname "org.sqlite.JDBC"
              :subprotocol "sqlite"
              :subname (env :database-url)
              :foreign_keys "on"})

(def query-conf {:connection db-spec
                 :identifiers ->kebab-case})

(defqueries "skuld/queries.sql")

(defn post-process [query-result]
  (transform-keys ->kebab-case query-result))

(defn insert!
  [conn table & rows]
  (map (keyword "last_insert_rowid()")
       (db/insert-multi! conn table (vec rows) {:entities ->snake_case})))

(defprotocol UserStorage
  (create-user [s username group-id] "Create a user belonging to a group"))

(defprotocol GroupStorage
  (list-groups [s] "List all groups")
  (create-group [s group-name users] "Create a group with users")
  (get-group [s group-id] "Get group with group id. Returns nil if not found.")
  (get-group-expenses [s group-id] "Get a list of the groups expenses"))

(defprotocol ExpenseStorage
  (create-expense [s group-id payed-by amount]
                  [s group-id payed-by amount date])
  (get-expense [s id]))

(defprotocol DeptStorage
  (create-dept [s user-name expense-id group-id amount])
  (get-user-dept [s user-name group-id])
  (get-dept [s group-id owned-by owed-to])
  (get-group-dept [s group-id]))

(declare merge-dept-pairs mk-dept-pair)
(defrecord Database [query-conf]
  UserStorage
  (create-user [d username group-id]
    (first (insert! (:connection query-conf)
                    :users {:name username
                            :group-id group-id})))

  GroupStorage
  (list-groups [d] (list-groups-query {} query-conf))

  (create-group [d group-name users]
    (if (empty? users)
      nil
      (let [group-id (str (java.util.UUID/randomUUID))]
        (insert! (:connection query-conf)
                 :groups {:id group-id
                          :name group-name})
        (doseq [username (set users)] (create-user d username group-id))
        group-id)))

  (get-group [d group-id]
    (let [result (get-group-query {:group_id group-id} query-conf)]
      (if (empty? result)
        nil
        {:id group-id
         :name (:group-name (first result))
         :members (vec (map :name result))})))

  (get-group-expenses [d group-id]
    (get-group-expenses-query {:group_id group-id} query-conf))

  ExpenseStorage
  (create-expense
    [d group-id payed-by amount]
    (create-expense d group-id payed-by amount (t/now)))

  (create-expense [d group-id payed-by amount date]
    (first (insert!
             (:connection query-conf)
             :expenses {:payed-by payed-by
                        :group-id group-id
                        :amount amount
                        :date (str date)})))

  (get-expense [d id]
    (let [expense (first (get-expense-query {:id id} query-conf))]
      (update expense :date #(f/parse date-format %))))

  DeptStorage
  (create-dept [d user-name expense-id group-id amount]
    (insert! (:connection query-conf)
             :dept {:user-name user-name
                    :expense-id expense-id
                    :group-id group-id
                    :amount amount}))

  (get-user-dept [d user-name group-id]
    (into {} (map (comp vec vals)
                  (get-user-with-dept-query {:user_name user-name
                                             :group_id group-id}
                                            query-conf))))

  (get-dept [d group-id owed-by owed-to]
    (get (get-user-dept d owed-by group-id) owed-to))

  (get-group-dept [d group-id]
    (let [dept (get-group-dept-query {:group_id group-id} query-conf)]
      (->> dept
           (map mk-dept-pair)
           merge-dept-pairs
           (filter #(not= (second %) 0.0))
           (into {})))))

(defn- mk-dept-pair [{:keys [owed-by owed-to amount]}]
  (let [pair-dept (if (< 0 (compare owed-by owed-to)) (* -1 amount) amount)]
    [(sorted-set owed-by owed-to) pair-dept]))

(defn- merge-dept-pairs
  "Merge dept pairs to get a sum of the dept between two users."
  [pairs]
  (let [f (fn [p [k v]] (update p k (fnil + 0) v))]
    (reduce f {} pairs)))

(defn add-shared-expense
  ([db group-id paying-user-name amount]
   (add-shared-expense db group-id paying-user-name amount
                       (:members (get-group db group-id))))
  ([db group-id paying-user-name amount split-between]
   (let [expense-id (create-expense db group-id paying-user-name amount)
         user-dept (/ (double amount) (count split-between))]
     (doseq [dept-user-name (remove #{paying-user-name} split-between)]
       (create-dept db dept-user-name expense-id group-id user-dept))
     expense-id)))
