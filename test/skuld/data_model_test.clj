(ns skuld.data-model-test
  (:require [camel-snake-kebab.core :refer [->kebab-case]]
            [clojure.java.jdbc :as db]
            [clojure.java.jdbc.deprecated :refer [with-connection]]
            [clojure.string :as s]
            [clojure.test :refer :all]
            [skuld.data-model :refer :all]))

(def database-schema-file "database_schema.sql")

(defn reset-database-fixture [f]
  (with-connection db-spec
    (doseq [sql (filter #(not (= % ""))
                        (map s/trim (s/split (slurp database-schema-file) #";")))]
      (db/db-do-prepared db-spec sql))
    (f)))

(use-fixtures :each reset-database-fixture)

(deftest creating-a-group
  (let [group-id (create-group "my group name" ["first-user"])]
    (is (= (:id (get-group group-id)) group-id))
    (is (= (:name (get-group group-id) "my group name")))
    (is (= (:members (get-group group-id)) ["first-user"]))))

(deftest adding-expense-creates-dept
  (let [group-id (create-group "Trysil" ["paying" "broke1" "broke2"])]
    (add-shared-expense group-id "paying" 15.0)
    (is (= (get-dept group-id "broke1" "paying") 5.0))
    (is (= (get-dept group-id "broke2" "paying") 5.0))
    (is (= (get-group-dept group-id) [{:owed-by "broke1"
                                       :owed-to "paying"
                                       :amount 5.0}
                                      {:owed-by "broke2"
                                       :owed-to "paying"
                                       :amount 5.0}]))))

(deftest smart-dept-calulation
  (let [group-id (create-group "Åre" ["user1" "user2" "user3"])]
    (add-shared-expense group-id "user1" 6.0)
    (add-shared-expense group-id "user2" 9.0)
    (add-shared-expense group-id "user3" 3.0)
    (is (= (calculate-dept (get-group-dept group-id))
           {#{"user1" "user2"} {:amount 1.0
                                :in-dept "user1"}
            #{"user1" "user3"} {:amount 1.0
                                :in-dept "user3"}
            #{"user2" "user3"} {:amount 2.0
                                :in-dept "user3"}}))))

(deftest no-dept-if-equal-expenses
  (let [group-id (create-group "Åre" ["user1" "user2"])]
    (add-shared-expense group-id "user1" 3.0)
    (add-shared-expense group-id "user2" 3.0)
    (is (= (calculate-dept (get-group-dept group-id)) {}))))

