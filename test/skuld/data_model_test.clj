(ns skuld.data-model-test
  (:require [clojure.java.jdbc :as db]
            [clojure.java.jdbc.deprecated :refer [with-connection]]
            [clojure.string :as s]
            [clojure.test :refer :all]
            [skuld.data-model :refer :all]))

(def database-schema-file "database_schema.sql")

(def test-db-spec (assoc db-spec :subname "file::memory:?cache=shared"))
(def test-query-conf (assoc query-conf :connection test-db-spec))

(defn reset-database-fixture [f]
  (with-redefs [db-spec test-db-spec
                query-conf test-query-conf]
    (with-connection db-spec
      (doseq [sql (filter #(not (= % ""))
                          (map s/trim (s/split (slurp database-schema-file) #";")))]
        (db/db-do-prepared db-spec sql))
      (f))))

(use-fixtures :each reset-database-fixture)

(def db (->Database))

(deftest creating-a-group
  (let [group-id (create-group db "my group name" ["first-user"])]
    (is (= (:id (get-group db group-id)) group-id))
    (is (= (:name (get-group db group-id)) "my group name"))
    (is (= (:members (get-group db group-id)) ["first-user"]))
    (is (= (:name (first (list-groups db))) "my group name"))))

(deftest list-expenses
  (let [group-id (create-group db "my group name" ["user1" "user2"])]
    (add-shared-expense db group-id "user1" 15.0)
    (add-shared-expense db group-id "user2" 20.0)
    (add-shared-expense db group-id "user2" 5.0)
    (is (= (map #(select-keys % [:name :amount])
                (get-group-expenses db group-id))
           [{:name "user1" :amount 15.0}
            {:name "user2" :amount 20.0}
            {:name "user2" :amount 5.0}]))))

(deftest adding-expense-creates-dept
  (let [group-id (create-group db "Trysil" ["paying" "broke1" "broke2"])]
    (add-shared-expense db group-id "paying" 15.0)
    (is (= (get-dept db group-id "broke1" "paying") 5.0))
    (is (= (get-dept db group-id "broke2" "paying") 5.0))
    (is (= (get-group-dept db group-id) {#{"broke1" "paying"} 5.0
                                         #{"broke2" "paying"} 5.0}))))

(deftest smart-dept-calulation
  (let [group-id (create-group db "Åre" ["user1" "user2" "user3"])]
    (add-shared-expense db group-id "user1" 6.0)
    (add-shared-expense db group-id "user2" 9.0)
    (add-shared-expense db group-id "user3" 3.0)
    (is (= (get-group-dept db group-id)
           {#{"user1" "user2"} 1.0
            #{"user1" "user3"} -1.0
            #{"user2" "user3"} -2.0}))))

(deftest no-dept-if-equal-expenses
  (let [group-id (create-group db "Åre" ["user1" "user2"])]
    (add-shared-expense db group-id "user1" 3.0)
    (add-shared-expense db group-id "user2" 3.0)
    (is (= (get-group-dept db group-id) {}))))
