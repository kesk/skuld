(ns skuld.data-model-test
  (:require [camel-snake-kebab.core :refer [->kebab-case]]
            [clojure.java.jdbc :as db]
            [clojure.java.jdbc.deprecated :refer [with-connection]]
            [clojure.string :as s]
            [clojure.test :refer :all]
            [skuld.data-model :refer :all]))

(def database-schema-file "database_schema.sql")

(def test-db-spec {:classname "org.sqlite.JDBC"
                   :subprotocol "sqlite"
                   :subname "file::memory:?cache=shared"
                   :foreign_keys "on"})

(def test-query-conf {:connection test-db-spec
                      :identifiers ->kebab-case})

(defn reset-database-fixture [f]
  (with-connection test-db-spec
    (doseq [sql (filter #(not (= % ""))
                        (map s/trim (s/split (slurp database-schema-file) #";")))]
      (db/db-do-prepared test-db-spec sql))
    (f)))

(defn redefs-fixture [f]
  (with-redefs [db-spec test-db-spec
                query-conf test-query-conf] (f)))

(use-fixtures :each reset-database-fixture redefs-fixture)

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
