(ns skuld.data.queries)

(defn- long-str [& strings]
  (clojure.string/join "\n" strings))

(defn get-expense [id]
  [(long-str
     "SELECT * FROM expenses"
     "JOIN users ON expenses.payed_by = users.id"
     "WHERE expenses.id = ?")
   id])

(def get-all-groups ["select * from groups"])

(defn get-group [group-id]
  [(long-str
     "SELECT u.*, g.name AS group_name FROM users AS u"
     "JOIN groups AS g ON u.group_id = g.id"
     "WHERE u.group_id = ?")
   group-id])

(defn get-user [id]
  [(long-str
     "SELECT users.*, groups.name AS group_name"
     "FROM users"
     "JOIN groups ON users.group_id = groups.id"
     "WHERE users.id = ?")
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
  [(long-str
     "SELECT d.user_name AS owed_by, e.payed_by AS owed_to, SUM(d.amount) AS amount"
     "FROM dept AS d"
     "JOIN expenses AS e ON d.expense_id = e.id"
     "WHERE d.group_id = ?"
     "GROUP BY owed_by, owed_to")
   group-id])

(defn get-group-expenses [group-id]
  [(long-str
     "SELECT id, payed_by AS name, amount, date"
     "FROM expenses"
     "WHERE group_id = ?"
     "ORDER BY date ASC")
   group-id])
