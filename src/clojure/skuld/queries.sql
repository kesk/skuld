-- name: get-expense-query
SELECT * FROM expenses
JOIN users ON expenses.payed_by = users.id
WHERE expenses.id = :id;

-- name: get-group-query
SELECT u.id, u.username, u.group_id, g.name AS group_name FROM users AS u
JOIN groups AS g ON u.group_id = g.id
WHERE g.id = :group_id;

-- name: get-group-members-query
SELECT * FROM users
WHERE group_id = :id;

-- name: get-user-query
SELECT
users.*,
groups.name AS group_name
FROM users
JOIN groups ON users.group_id = groups.id
WHERE users.id = :id;

--name: get-user-with-dept-query
SELECT e.payed_by AS owed_to, SUM(d.amount) AS amount
FROM dept AS d
JOIN expenses AS e ON d.expense_id = e.id
WHERE d.user_id = :user_id
GROUP BY owed_to

--name: get-group-dept-query
SELECT d.user_id AS owed_by, e.payed_by AS owed_to, SUM(d.amount) AS amount
FROM dept AS d
JOIN expenses AS e ON d.expense_id = e.id
WHERE d.group_id = :group_id
GROUP BY owed_by, owed_to
