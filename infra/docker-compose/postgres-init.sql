-- Create service-owned databases only. No users, grants, topics, or business tables.
SELECT format('CREATE DATABASE %I', name)
FROM (VALUES ('auth_db'), ('transfer_db'), ('payment_db'), ('settlement_db'), ('swift_db'),
             ('wealth_report_db'), ('aml_db'), ('ai_gateway_db'), ('fep_adapter_db')) AS services(name)
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname=name)
\gexec
