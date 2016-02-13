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
  (let [group-id (create-group "my group name")]
    (is (= (:id (get-group group-id)) group-id))
    (is (= (:name (get-group group-id) "my group name")))))

(deftest creating-a-group-with-user
  (let [group-id (create-group "foo group")
        user-id (create-user "foouser" group-id)]
    (is (= (:username (get-user user-id)) "foouser"))
    (is (= (:group-id (get-user user-id)) group-id))
    (is (= (map :id (get-group-members group-id)) [user-id]))))

(deftest adding-expense-creates-dept
  (let [group-id (create-group "Trysil")
        paying-user-id (create-user "paying" group-id)
        broke-user1-id (create-user "broke1" group-id)
        broke-user2-id (create-user "broke2" group-id)]
    (add-shared-expense group-id paying-user-id 15.0)
    (is (= (get-dept broke-user1-id paying-user-id) 5.0))
    (is (= (get-dept broke-user2-id paying-user-id) 5.0))
    (is (= (get-group-dept group-id) [{:owed-by broke-user1-id
                                       :owed-to paying-user-id
                                       :amount 5.0}
                                      {:owed-by broke-user2-id
                                       :owed-to paying-user-id
                                       :amount 5.0}]))))

(deftest smart-dept-calulation
  (let [group-id (create-group "Åre")
        user1 (create-user "user1" group-id)
        user2 (create-user "user2" group-id)
        user3 (create-user "user3" group-id)]
    (add-shared-expense group-id user1 6.0)
    (add-shared-expense group-id user2 9.0)
    (add-shared-expense group-id user3 3.0)
    (is (= (calculate-dept (get-group-dept group-id)) {#{1 2} {:amount 1.0
                                                               :in-dept 1}
                                                       #{1 3} {:amount 1.0
                                                               :in-dept 3}
                                                       #{2 3} {:amount 2.0
                                                               :in-dept 3}}))))

(deftest no-dept-if-equal-expenses
  (let [group-id (create-group "Åre")
        user1 (create-user "user1" group-id)
        user2 (create-user "user2" group-id)]
    (add-shared-expense group-id user1 3.0)
    (add-shared-expense group-id user2 3.0)
    (is (= (calculate-dept (get-group-dept group-id)) {}))))

