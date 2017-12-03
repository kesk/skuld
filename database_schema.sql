CREATE TABLE user_group (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL);

CREATE TABLE user (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    group_id TEXT NOT NULL,
    FOREIGN KEY (group_id) REFERENCES user_group (id));

CREATE UNIQUE INDEX unique_name_group_id ON user (name, group_id);

CREATE TABLE expense (
    id INTEGER PRIMARY KEY,
    user_id INTEGER NOT NULL,
    group_id INTEGER NOT NULL,
    amount REAL NOT NULL,
    created TEXT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES user (id),
    FOREIGN KEY (group_id) REFERENCES user_group (id));

CREATE TABLE expense_share (
    id INTEGER PRIMARY KEY,
    expense_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    group_id TEXT NOT NULL,
    amount REAL NOT NULL,
    FOREIGN KEY (user_id) REFERENCES user (id),
    FOREIGN KEY (expense_id) REFERENCES expense (id));

CREATE TABLE dept (
    id INTEGER PRIMARY KEY,
    from_user INTEGER NOT NULL,
    to_user INTEGER NOT NULL,
    amount REAL NOT NULL,
    group_id TEXT NOT NULL,
    FOREIGN KEY (from_user) REFERENCES user (id),
    FOREIGN KEY (to_user) REFERENCES user (id),
    FOREIGN KEY (group_id) REFERENCES user_group (id));
