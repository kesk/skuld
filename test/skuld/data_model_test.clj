(ns skuld.data-model-test
  (:require [clojure.java.jdbc :refer [db-do-prepared with-db-connection]]
            [clojure.string :as s]
            [clojure.test :refer :all]
            [skuld.data-model :refer :all]))

(def database-schema-file "database_schema.sql")

(def test-query-conf
  (assoc-in query-conf [:connection :subname] "file::memory:?cache=shared"))

(def ^:dynamic *db* (->Database test-query-conf))

(defn reset-database-fixture [f]
  (with-db-connection [db-spec (:connection test-query-conf)]
    (doseq [sql (->> (s/split (slurp database-schema-file) #";")
                     (map s/trim)
                     (remove #{""}))]
      (db-do-prepared db-spec sql))
    (binding [*db* (assoc-in *db* [:query-conf :connection] db-spec)]
      (f))))

(use-fixtures :each reset-database-fixture)

(deftest creating-a-group
  (let [group-id (create-group *db* "my group name" ["first-user"])]
    (is (= (:id (get-group *db* group-id)) group-id))
    (is (= (:name (get-group *db* group-id)) "my group name"))
    (is (= (:members (get-group *db* group-id)) ["first-user"]))
    (is (= (:name (first (list-groups *db*))) "my group name"))))

(deftest list-expenses
  (let [group-id (create-group *db* "my group name" ["user1" "user2"])]
    (add-shared-expense *db* group-id "user1" 15.0)
    (add-shared-expense *db* group-id "user2" 20.0)
    (add-shared-expense *db* group-id "user2" 5.0)
    (is (= (map #(select-keys % [:name :amount])
                (get-group-expenses *db* group-id))
           [{:name "user1" :amount 15.0}
            {:name "user2" :amount 20.0}
            {:name "user2" :amount 5.0}]))))

(deftest adding-expense-creates-dept
  (let [group-id (create-group *db* "Trysil" ["paying" "broke1" "broke2"])]
    (add-shared-expense *db* group-id "paying" 15.0)
    (is (= (get-dept *db* group-id "broke1" "paying") 5.0))
    (is (= (get-dept *db* group-id "broke2" "paying") 5.0))
    (is (= (get-group-dept *db* group-id) {#{"broke1" "paying"} 5.0
                                         #{"broke2" "paying"} 5.0}))))

(deftest smart-dept-calulation
  (let [group-id (create-group *db* "Åre" ["user1" "user2" "user3"])]
    (add-shared-expense *db* group-id "user1" 6.0)
    (add-shared-expense *db* group-id "user2" 9.0)
    (add-shared-expense *db* group-id "user3" 3.0)
    (is (= (get-group-dept *db* group-id)
           {#{"user1" "user2"} 1.0
            #{"user1" "user3"} -1.0
            #{"user2" "user3"} -2.0}))))

(deftest no-dept-if-equal-expenses
  (let [group-id (create-group *db* "Åre" ["user1" "user2"])]
    (add-shared-expense *db* group-id "user1" 3.0)
    (add-shared-expense *db* group-id "user2" 3.0)
    (is (= (get-group-dept *db* group-id) {}))))
