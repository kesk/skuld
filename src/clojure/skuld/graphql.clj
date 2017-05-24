(ns skuld.graphql
  (:require [camel-snake-kebab.core :refer [->kebab-case]]
            [claro.data :as data]
            [claro.engine :as engine]
            [environ.core :refer [env]]
            [manifold.deferred :as d]
            [yesql.core :refer [defqueries]]))

(def db-spec {:classname "org.sqlite.JDBC"
              :subprotocol "sqlite"
              :subname (env :database-url)
              :foreign_keys "on"})

(def query-conf {:connection db-spec
                 :identifiers ->kebab-case})

(defqueries "skuld/graphql.sql" query-conf)

(def schema
  "type Group { id: ID!, name: String!, members: [String!]! }
   type QueryRoot { group(id: ID!): Group }
   schema { query: QueryRoot }")

(get-group-query {:id "351ca88f-31cd-44ea-b077-1b80e0a4cb89"})

(defrecord GroupMembers [id]
  data/Resolvable
  (resolve! [_ _]
    (get-group-members-query {:id id}))

  data/Transform
  (transform [_ members]
     (map :name members)))

(defrecord GroupExpenses [id]
  data/Resolvable
  (resolve! [_ _]
    (get-group-expenses-query {:id id} query-conf)))

(defrecord Group [id]
  data/Resolvable
  (resolve! [_ _]
    (d/future
      (first (get-group-query {:id id} query-conf))))

  data/Transform
  (transform [_ group]
    (some-> group
            (assoc :members (->GroupMembers id))
            (assoc :expenses (->GroupExpenses id)))))

(engine/run!! (->Group "351ca88f-31cd-44ea-b077-1b80e0a4cb89"))
(engine/run!! (->Group "8fa524d4-f01d-4db7-8488-d4851b34466a"))
(engine/run!! (->GroupExpenses "8fa524d4-f01d-4db7-8488-d4851b34466a"))
(engine/run! (->GroupMembers "351ca88f-31cd-44ea-b077-1b80e0a4cb89"))
