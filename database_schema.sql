CREATE TABLE dept (
    id INTEGER PRIMARY KEY,
    user_name TEXT NOT NULL,
    group_id TEXT NOT NULL,
    expense_id INTEGER NOT NULL,
    amount REAL NOT NULL,
    FOREIGN KEY(user_name, group_id) REFERENCES users(name, group_id),
    FOREIGN KEY(group_id) REFERENCES groups(id),
    FOREIGN KEY(expense_id) REFERENCES expenses(id));

CREATE TABLE users (
    name TEXT NOT NULL,
    group_id TEXT NOT NULL,
    PRIMARY KEY (name, group_id),
    FOREIGN KEY(group_id) REFERENCES groups(id));

CREATE TABLE groups (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL);

CREATE TABLE expenses (
    id INTEGER PRIMARY KEY,
    payed_by TEXT NOT NULL,
    group_id TEXT NOT NULL,
    amount REAL NOT NULL,
    date TEXT NOT NULL,
    FOREIGN KEY(payed_by, group_id) REFERENCES users(name, group_id));
