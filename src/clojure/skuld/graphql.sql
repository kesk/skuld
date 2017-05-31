-- name: get-group-query
SELECT * FROM groups WHERE id = :id;

-- name: get-group-members-query
SELECT name FROM users WHERE group_id = :id;

-- name: get-group-expenses-query
SELECT id, payed_by, amount, date AS date_added FROM expenses WHERE group_id = :id;

-- name: get-group-dept-query
SELECT
d.user_name AS owed_by,
e.payed_by AS owed_to,
SUM(d.amount) AS amount
FROM dept AS d
JOIN expenses AS e ON d.expense_id = e.id
WHERE d.group_id = :id
GROUP BY owed_by, owed_to;
