#!/bin/sh
set -eu
: "${APP_DB_PASSWORD:?Set APP_DB_PASSWORD}"
psql --username "$POSTGRES_USER" --dbname postgres --set=ON_ERROR_STOP=1 \
    --set=app_password="$APP_DB_PASSWORD" <<'SQL'
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', service || '_app', :'app_password')
FROM (VALUES ('auth'), ('transfer'), ('payment'), ('settlement'), ('swift'),
             ('wealth_report'), ('aml'), ('ai_gateway'), ('fep_adapter')) AS services(service)
\gexec
SELECT format('CREATE DATABASE %I OWNER %I', service || '_db', service || '_app')
FROM (VALUES ('auth'), ('transfer'), ('payment'), ('settlement'), ('swift'),
             ('wealth_report'), ('aml'), ('ai_gateway'), ('fep_adapter')) AS services(service)
\gexec
SQL
