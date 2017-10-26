(ns skuld.graphql
  (:require [alumbra.core :as alumbra]
            [claro.data :as data]
            [claro.engine :as engine]
            [environ.core :refer [env]]
            [manifold.deferred :as d]
            [yesql.core :refer [defqueries]]))

(def db-spec {:classname "org.sqlite.JDBC"
              :subprotocol "sqlite"
              :subname (env :database-url)
              :foreign_keys "on"})

(def query-conf {:connection db-spec})

(defqueries "skuld/graphql.sql" query-conf)

(def schema
  "type Expense { id: ID!, payed_by: String!, amount: Float!, data_added: String! }
  type Dept { id: ID!, user_name: String!, amount: Float!, expense: Expense! }
  type Group { id: ID!, name: String!, members: [String!]!, expenses: [Expense!]! }
  type QueryRoot { group(id: ID!): Group }
  schema { query: QueryRoot }")

(defrecord Expense [id]
  data/Resolvable
  data/BatchedResolvable
  (resolve-batch! [_ _ expenses]
    (d/future
      (get-expenses-query {:ids (map :id expenses)} query-conf))))

(defrecord GroupMembers [id]
  data/Resolvable
  (resolve! [_ _]
    (get-group-members-query {:id id} query-conf))

  data/Transform
  (transform [_ members]
     (map :name members)))

(defrecord GroupExpenses [id]
  data/Resolvable
  (resolve! [_ _]
    (get-group-expenses-query {:id id} query-conf)))

(defrecord GroupDept [id]
  data/Resolvable
  (resolve! [_ _]
    (get-group-dept-query {:id id} query-conf))

  data/Transform
  (transform [_ depts]
    (let [insert-expense (fn [dept]
                           (-> dept
                               (assoc :expense (->Expense (:expense_id dept)))
                               (dissoc :expense_id)))]
      (map insert-expense depts))))

(defrecord Group [id]
  data/Resolvable
  (resolve! [_ _]
    (d/future
      (first (get-group-query {:id id} query-conf))))

  data/Transform
  (transform [_ group]
    (some-> group
            (assoc :members (->GroupMembers id))
            (assoc :expenses (->GroupExpenses id))
            (assoc :dept (->GroupDept id)))))

(def QueryRoot
  {:group (map->Group {})})

(def graphql-handler
  (alumbra/handler
    {:schema schema
     :query QueryRoot}))

#_(pprint (engine/run!! (->Group "351ca88f-31cd-44ea-b077-1b80e0a4cb89")))
#_(engine/run!! (->GroupExpenses "351ca88f-31cd-44ea-b077-1b80e0a4cb89"))
#_(engine/run!! (->GroupDept "351ca88f-31cd-44ea-b077-1b80e0a4cb89"))
#_(engine/run!! (->GroupMembers "351ca88f-31cd-44ea-b077-1b80e0a4cb89"))
(engine/run!! (->Expense 1))
