-- name: get-group-query
SELECT * FROM groups WHERE id = :id;

-- name: get-group-members-query
SELECT name FROM users WHERE group_id = :id;

-- name: get-group-expenses-query
SELECT id, payed_by, amount, date AS date_added FROM expenses WHERE group_id = :id;

-- name: get-expenses-query
SELECT * FROM expenses WHERE id IN (:ids);

-- name: get-group-dept-query
SELECT id, user_name, expense_id, SUM(amount) AS amount
FROM dept
WHERE group_id = :id
GROUP BY user_name;
