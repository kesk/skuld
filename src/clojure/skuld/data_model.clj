(ns skuld.data-model
  (:require [camel-snake-kebab.core :refer [->kebab-case ->snake_case]]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clojure.java.jdbc :as jdbc]
            [clojure.spec.alpha :as s]
            [environ.core :refer [env]]
            [skuld.common :refer [date-format]]
            [skuld.data.queries :as q]))

(def db-spec {:classname "org.sqlite.JDBC"
              :subprotocol "sqlite"
              :subname (env :sqlite-database-file)
              :foreign_keys "on"})

(defn post-process [query-result]
  (transform-keys ->kebab-case query-result))

(defprotocol Database
  (insert! [d table rows])
  (delete! [d table where-clause])
  (run-query [d sql-params])
  (with-transaction [d func]))

(defrecord SQLiteDatabase
  [classname
   subprotocol
   subname
   foreign_keys]

  Database
  (insert! [d table rows]
    (map (keyword "last_insert_rowid()")
         (jdbc/insert-multi! d table rows {:entities ->snake_case})))

  (delete! [d table where-clause]
    (jdbc/delete! d table where-clause {:entities ->snake_case}))

  (run-query [d sql-params]
    (jdbc/query d sql-params {:identifiers ->kebab-case}))

  (with-transaction [d func]
    (jdbc/db-transaction* d func)))

;UserStorage
(defn create-user [db username group-id]
  (first (insert! db :user
                  [{:name username
                    :group-id group-id}])))

;GroupStorage
(defn list-groups [db]
  (run-query db q/get-all-groups))

(defn create-group [db group-name users]
  (if (empty? users)
    nil
    (let [group-id (str (java.util.UUID/randomUUID))]
      (insert! db :user-group
               [{:id group-id
                 :name group-name}])
      (doseq [username (set users)] (create-user db username group-id))
      group-id)))

(s/def ::user-id number?)
(s/def ::user-name string?)
(s/def ::group-id string?)
(s/def ::group-name string?)
(s/def ::group-member (s/keys :req [::user-id ::user-name]))
(s/def ::group-members (s/coll-of ::group-member))
(s/def ::group (s/keys :req [::group-id ::group-name ::group-members]))

(defn get-group [db id]
  (let [group (first (run-query db (q/get-group id)))
        members (run-query db (q/get-group-members id))]
    (if (nil? group)
      nil
      {:id (:id group)
       :name (:name group)
       :members members})))

;DeptStorage
(declare dist-dept)

(defn get-user-totals [db group-id]
  (->> (q/get-total-amount-per-user group-id)
       (run-query db)
       (reduce #(assoc %1 (:user-id %2) (:amount %2)) {})))

(defn calculate-group-dept [db group-id]
  (let [user-total (get-user-totals db group-id)
        in-dept (->> user-total
                     (filter #(< 0 (second %)))
                     (sort-by second >))
        filter-loaners (comp (filter #(> 0 (second %)))
                             (map #(vector (first %) (* -1 (second %)))))
        loaners (->> user-total
                     (into [] filter-loaners)
                     (sort-by second >))]
    (dist-dept loaners in-dept)))

(defn- dist-dept [[l & loaners] [i & in-dept]]
  (if (some nil? [l i])
    []
    (let [id first
          amount second]
      (case (compare (amount l) (amount i))
        -1 (let [dept (- (amount i) (amount l))
                 remainder [(id i) (- (amount i) dept)]]
             (cons [(id i) (id l) dept]
                   (dist-dept loaners (cons remainder in-dept))))

        0 (cons [(id i) (id l) (amount l)] (dist-dept loaners in-dept))

        1 (let [dept (- (amount l) (amount i))
                remainder [(id l) (- (amount l) dept)]]
            (cons [(id i) (id l) (amount i)]
                  (dist-dept (cons remainder  loaners) in-dept)))))))

(defn clear-dept [db group-id]
  (delete! db :dept ["group_id = ?" group-id]))

(defn save-dept [db group-id dept]
  (let [create-rows (comp (map #(zipmap [:from-user :to-user :amount] %))
                          (map #(assoc % :group-id group-id)))]
    (insert! db :dept (into [] create-rows dept))))

(defn get-group-dept [db group-id]
  (run-query db (q/get-group-dept group-id)))

;ExpenseStorage
(defn create-expense
  ([db group-id payed-by shared-with amount]
   (create-expense db group-id payed-by shared-with amount (t/now)))

  ([db group-id payed-by shared-with amount date]
   (with-transaction
     db
     (fn [db-trans]
       (let [share (/ (double amount) (count shared-with))
             expense-id (first (insert! db-trans :expense
                                        [{:user-id payed-by
                                          :group-id group-id
                                          :amount amount
                                          :created (str date)}]))
             share-rows (map (fn [user-id]
                               {:expense-id expense-id
                                :user-id user-id
                                :group-id group-id
                                :amount share})
                             shared-with)]
         (insert! db-trans :expense-share share-rows)
         ; Maybe use async channel to oupdate dept?
         (clear-dept db-trans group-id)
         (save-dept db-trans group-id (calculate-group-dept db-trans group-id))
         expense-id)))))

(defn get-expense [db id]
  (let [expense (first (run-query db (q/get-expense id)))]
    (update expense :created (partial f/parse date-format))))

(defn get-expense-shares [db expense-id]
  (run-query db (q/get-expense-shares expense-id)))

(defn get-group-expenses [db group-id]
  (run-query db (q/get-group-expenses group-id)))

