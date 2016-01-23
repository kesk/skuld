CREATE TABLE dept (
    id INTEGER PRIMARY KEY,
    user_id INTEGER NOT NULL,
    group_id TEXT NOT NULL,
    expense_id INTEGER NOT NULL,
    amount REAL NOT NULL,
    FOREIGN KEY(user_id) REFERENCES users(id),
    FOREIGN KEY(group_id) REFERENCES groups(id),
    FOREIGN KEY(expense_id) REFERENCES expenses(id));

CREATE TABLE users (
    id INTEGER PRIMARY KEY,
    username TEXT NOT NULL,
    group_id TEXT NOT NULL,
    FOREIGN KEY(group_id) REFERENCES groups(id));

CREATE TABLE groups (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL);

CREATE TABLE expenses (
    id INTEGER PRIMARY KEY,
    payed_by INTEGER NOT NULL,
    amount REAL NOT NULL,
    date TEXT NOT NULL,
    FOREIGN KEY(payed_by) REFERENCES users(id));
