-- name: get-group-query
SELECT * FROM groups WHERE id = :id;

-- name: get-group-members-query
SELECT name FROM users WHERE group_id = :group_id;
