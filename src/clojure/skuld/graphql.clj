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

(defn- mk-dept-pair [{:keys [owed-by owed-to amount]}]
  (let [pair-dept (if (< 0 (compare owed-by owed-to)) (* -1 amount) amount)]
    [(sorted-set owed-by owed-to) pair-dept]))

(defn- merge-dept-pairs
  "Merge dept pairs to get a sum of the dept between two users."
  [pairs]
  (let [f (fn [p [k v]] (update p k (fnil + 0) v))]
    (reduce f {} pairs)))

(defn- format-dept [[pair amount]]
  (let [owed-by (if (< 0 amount) (first pair) (second pair))
        owed-to (if (< 0 amount) (second pair) (first pair))]
    {:owed-by owed-by
     :owed-to owed-to
     :amount (if (< 0 amount) amount (* -1 amount))}))

(defrecord GroupDept [id]
  data/Resolvable
  (resolve! [_ _]
    (get-group-dept-query {:id id} query-conf))

  data/Transform
  (transform [_ dept]
    (->> dept
        (map mk-dept-pair)
        merge-dept-pairs
        (filter #(not= (second %) 0.0))
        (map format-dept))))

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
(engine/run!! (->GroupExpenses "351ca88f-31cd-44ea-b077-1b80e0a4cb89"))
(engine/run!! (->GroupDept "351ca88f-31cd-44ea-b077-1b80e0a4cb89"))
(engine/run! (->GroupMembers "351ca88f-31cd-44ea-b077-1b80e0a4cb89"))
