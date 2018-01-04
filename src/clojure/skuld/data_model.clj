(ns skuld.data-model
  (:require [camel-snake-kebab.core :refer [->kebab-case ->snake_case]]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [clojure.java.jdbc :as jdbc]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [environ.core :refer [env]]
            [skuld.data.queries :as q])
  (:import [java.time Instant]))

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

(defn get-group [db id]
  (let [group (first (run-query db (q/get-group id)))
        members (run-query db (q/get-group-members id))]
    (if (nil? group)
      nil
      {:id (:id group)
       :name (:name group)
       :members members})))

;Dept
(defn get-user-totals [db group-id]
  (->> (q/get-total-amount-per-user group-id)
       (run-query db)
       (reduce #(assoc %1 (:user-id %2) (:amount %2)) {})))

(declare dist-dept)
(defn calculate-group-dept [db group-id]
  (let [user-total (get-user-totals db group-id)
        in-dept (->> user-total
                     (filter #(< 0 (second %)))
                     (sort-by second >))
        filter-lenders (comp (filter #(> 0 (second %)))
                             (map #(vector (first %) (* -1 (second %)))))
        lenders (->> user-total
                     (into [] filter-lenders)
                     (sort-by second >))]
    (dist-dept lenders in-dept)))

(defn- dist-dept [lenders in-dept]
  (loop [[l & ls] lenders
         [i & is] in-dept
         total []]
    (if (some nil? [l i])
      total
      (let [id first
            amount second]
        (case (compare (amount l) (amount i))
          -1 (let [dept (- (amount i) (amount l))
                   remainder [(id i) (- (amount i) dept)]]
               (recur ls (cons remainder is) (cons [(id i) (id l) dept] total)))

          0 (recur ls is (cons [(id i) (id l) (amount l)] total))

          1 (let [dept (- (amount l) (amount i))
                  remainder [(id l) (- (amount l) dept)]]
              (recur (cons remainder ls) is
                     (cons [(id i) (id l) (amount i)] total))))))))

(s/fdef dist-dept
        :args (s/cat :lenders (s/coll-of (s/coll-of number? :count 2))
                     :in-dept (s/coll-of (s/coll-of number? :count 2)))

        :ret (s/coll-of (s/coll-of number? :count 3))

        :fn (fn [a] (= (->> a :ret (map #(get % 3)) (reduce +))
                       (max (-> a :lenders (map second) (reduce +))
                            (-> a :in-dept (map second) (reduce +))))))

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
   (create-expense db group-id payed-by shared-with amount (Instant/now)))

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
         expense-id)))))

(s/fdef create-expense
        :args (s/or :without-date (s/cat :db #(satisfies? Database %)
                                         :group-id string?
                                         :payed-by number?
                                         :shared-with (s/coll-of number?)
                                         :amount number?)
                    :with-date (s/cat :db #(satisfies? Database %)
                                      :group-id string?
                                      :payed-by number?
                                      :shared-with (s/coll-of number?)
                                      :amount number?
                                      :date inst?)))

(defn get-expense [db id]
  (let [expense (first (run-query db (q/get-expense id)))]
    (update expense :created #(Instant/parse %))))

(defn get-expense-shares [db expense-id]
  (run-query db (q/get-expense-shares expense-id)))

(defn get-group-expenses [db group-id]
  (run-query db (q/get-group-expenses group-id)))

(stest/instrument)
