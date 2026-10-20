-- Optional persistent-stack upgrade only. Run in postgres with psql --set app_password=<existing APP_DB_PASSWORD>.
-- Creates only the missing synthetic PG database/role. Never rotates an existing role or changes business data.
\set ON_ERROR_STOP on
SELECT format('CREATE ROLE mock_pg_app LOGIN PASSWORD %L', :'app_password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname='mock_pg_app')
\gexec
SELECT 'CREATE DATABASE mock_pg_db OWNER mock_pg_app'
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname='mock_pg_db')
\gexec
