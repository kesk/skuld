#!/bin/bash

file_name=database.sqlite
schema=database_schema.sql

if [[ -e $file_name && $1 != "-f" ]]; then
    echo "Database exists! Exiting..."
    exit 1
fi

if [[ $1 == "-f" ]]; then
    echo "Removing old database"
    rm $file_name
fi

echo "Creating new database from schema $schema"
sqlite3 $file_name < $schema
