(ns skuld.data-model-test
  (:require [clojure.java.jdbc :refer [db-do-prepared with-db-connection]]
            [clojure.string :as s]
            [clojure.test :refer :all]
            [skuld.data-model :refer :all]))

(def database-schema-file "database_schema.sql")

(def test-db-spec (assoc db-spec :subname "file::memory:?cache=shared"))

(def db (map->SQLiteDatabase test-db-spec))

(defn reset-database-fixture [test-body]
  (with-db-connection [db-spec test-db-spec]
    (doseq [sql (->> (s/split (slurp database-schema-file) #";")
                     (map s/trim)
                     (remove #{""}))]
      (db-do-prepared db-spec sql))
    (test-body)))

(use-fixtures :each reset-database-fixture)

(deftest ^:it creating-a-group
  (let [group-id (create-group db "my group name" ["first-user"])
        created-group (get-group db group-id)]
    (is (= (:id created-group) group-id))
    (is (= (:name created-group) "my group name"))
    (is (= (map :name (:members created-group)) ["first-user"]))
    (is (= (:name (first (list-groups db))) "my group name"))))

(defn user-id-map [users]
  (into {} (map (fn [user] [(keyword (:name user)) (:id user)]) users)))

(deftest ^:it create-shared-expense
  (let [group-id (create-group db "Group" ["user1" "user2" "user3"])
        group (get-group db group-id)
        users (user-id-map (:members group))
        user1 (:user1 users)
        user2 (:user2 users)
        user3 (:user3 users)
        expense-id1 (create-expense db group-id user1 [user1 user2 user3] 9.0)
        expense-id2 (create-expense db group-id user2 [user1 user2 user3] 9.0)
        expense (get-expense db expense-id1)
        expense-shares (get-expense-shares db expense-id1)
        group-dept (map #(select-keys % [:from-user :to-user :amount])
                        (get-group-dept db group-id))]
    (is (= (:user-id expense) user1))
    (is (= (:amount expense) 9.0))
    (let [es (map #(select-keys % [:user-id :amount]) expense-shares)]
      (is (= (count es) 3))
      (is (every? (set es) [{:user-id user1 :amount 3.0}
                            {:user-id user2 :amount 3.0}
                            {:user-id user3 :amount 3.0}])))
    (is (= (count group-dept) 2))
    (is (every? (set group-dept) [{:from-user user3
                                   :to-user user1
                                   :amount 3.0}
                                  {:from-user user3
                                   :to-user user2
                                   :amount 3.0}]))))
