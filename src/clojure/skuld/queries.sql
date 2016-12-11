-- name: get-expense-query
SELECT * FROM expenses
JOIN users ON expenses.payed_by = users.id
WHERE expenses.id = :id;

-- name: get-group-query
SELECT u.*, g.name AS group_name FROM users AS u
JOIN groups AS g ON u.group_id = g.id
WHERE u.group_id = :group_id;

-- name: list-groups-query
SELECT * FROM groups;

-- name: get-user-query
SELECT
users.*,
groups.name AS group_name
FROM users
JOIN groups ON users.group_id = groups.id
WHERE users.id = :id;

-- name: get-user-with-dept-query
SELECT e.payed_by AS owed_to, SUM(d.amount) AS amount
FROM dept AS d
JOIN expenses AS e ON d.expense_id = e.id
WHERE d.user_name = :user_name AND d.group_id = :group_id
GROUP BY owed_to;

-- name: get-group-dept-query
SELECT d.user_name AS owed_by, e.payed_by AS owed_to, SUM(d.amount) AS amount
FROM dept AS d
JOIN expenses AS e ON d.expense_id = e.id
WHERE d.group_id = :group_id
GROUP BY owed_by, owed_to;

-- name: get-group-expenses-query
SELECT id, payed_by AS name, amount, date
FROM expenses
WHERE group_id = :group_id
ORDER BY date ASC
