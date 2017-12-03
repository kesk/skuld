(ns skuld.data.queries)

(defn- long-str [& strings]
  (clojure.string/join "\n" strings))

(def get-all-groups ["select * from user_group"])

(defn get-group [id]
  ["SELECT * FROM user_group WHERE id = ?" id])

(defn get-group-members [id]
  ["SELECT id, name FROM user WHERE group_id = ?" id])

(defn get-expense [id]
  ["SELECT * FROM expense WHERE id = ?" id])

(defn get-expense-shares [expense-id]
  [(long-str
     "SELECT * FROM expense_share"
     "WHERE expense_id = ?")
   expense-id])

(defn get-group-expenses [group-id]
  [(long-str
     "SELECT e.id, e.user_id, u.name, e.group_id, e.amount"
     "FROM expense e"
     "JOIN user u ON e.user_id = u.id"
     "WHERE group_id = ?"
     "ORDER BY date ASC")
   group-id])

(defn get-sum-expenses [group-id]
  [(long-str
     "SELECT e.user_id id, u.name name, sum(e.amount) amount"
     "FROM expense e"
     "JOIN user u ON e.user_id = u.id"
     "WHERE e.group_id = ?"
     "GROUP BY e.user_id")
   group-id])

(defn get-sum-expense-shares [group-id]
  [(long-str
     "SELECT es.user_id id, u.name name, sum(es.amount) amount"
     "FROM expense_share es"
     "JOIN user u ON es.user_id = u.id"
     "WHERE es.group_id = ?"
     "GROUP BY es.user_id")
   group-id])

(defn get-user [id]
  [(long-str
     "SELECT u.*, g.name AS group_name"
     "FROM user u"
     "JOIN user_group g ON u.group_id = g.id"
     "WHERE u.id = ?")
   id])

(defn get-user-with-dept [username group-id]
  [(long-str
     "SELECT e.payed_by AS owed_to, SUM(d.amount) AS amount"
     "FROM dept AS d"
     "JOIN expenses AS e ON d.expense_id = e.id"
     "WHERE d.user_name = ? AND d.group_id = ?"
     "GROUP BY owed_to")
   username group-id])

(defn get-group-dept [group-id]
  ["SELECT * FROM dept WHERE group_id = ?" group-id])

