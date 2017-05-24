-- name: get-group-query
SELECT * FROM groups WHERE id = :id;

-- name: get-group-members-query
SELECT name FROM users WHERE group_id = :id;

-- name: get-group-expenses-query
SELECT id, payed_by, amount, date FROM expenses WHERE group_id = :id;
