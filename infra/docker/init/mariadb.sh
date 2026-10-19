#!/bin/sh
set -eu
# The generated local password is hex, so interpolation cannot change the SQL syntax.
case "${APP_DB_PASSWORD:-}" in ''|*[!0-9a-fA-F]*) echo "APP_DB_PASSWORD must be hexadecimal." >&2; exit 1 ;; esac
MYSQL_PWD="$MARIADB_ROOT_PASSWORD" mariadb --protocol=socket --user=root <<SQL
CREATE DATABASE IF NOT EXISTS account_db CHARACTER SET utf8mb4 COLLATE utf8mb4_nopad_bin;
CREATE DATABASE IF NOT EXISTS ledger_db CHARACTER SET utf8mb4 COLLATE utf8mb4_nopad_bin;
CREATE USER IF NOT EXISTS 'account_app'@'%' IDENTIFIED BY '$APP_DB_PASSWORD';
CREATE USER IF NOT EXISTS 'ledger_app'@'%' IDENTIFIED BY '$APP_DB_PASSWORD';
GRANT ALL PRIVILEGES ON account_db.* TO 'account_app'@'%';
GRANT ALL PRIVILEGES ON ledger_db.* TO 'ledger_app'@'%';
SQL
