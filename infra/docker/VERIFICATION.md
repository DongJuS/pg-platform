# Docker verification — 2026-10-18

Verified on Docker Engine 29.8.0 / Compose 5.5.1, Linux ARM64 containers.
The Docker VM has about 7.75 GiB RAM; app startup was checked in groups rather
than claiming all 16 plus the pre-existing stack fit simultaneously.

- All 16 service images built successfully from the multi-stage Dockerfile.
  After narrowing the Dockerfile-specific build context, the context transfer was
  approximately 176 kB; local build outputs and the Terraform provider cache were excluded.
- All 16 images use UID/GID 10001:10001. Their image environment contains neither
  the local DB passwords nor an active Spring profile.
- All 16 applications returned HTTP 200 / status UP through the localhost TCP
  forwarder: Gateway/Auth/Account/Ledger/Transfer; Payment/Settlement/SWIFT/
  Wealth Report/AML/AI Gateway/FEP; and the four mocks.
- Auth integration in the new database: register 201, duplicate registration 409,
  login and refresh 200, logout 204. Credentials and token values were not logged.
- Gateway still rejects a protected route (401/403). The unfinished Transfer read
  use case still reports 501; Docker does not introduce a fake successful transaction.
- PostgreSQL initialized nine service databases. MariaDB initialized account_db
  and ledger_db. Kafka initialized the five listed existing event topic names.
- Prometheus and Grafana were reachable on localhost; mock-pg exposed Prometheus metrics.
- Compose validation, shell/Python syntax, private env-file permissions and refusal
  to overwrite an existing env file passed.

The first runtime checks found and corrected tmpfs option quoting and the lack of
host port publishing on fully internal networks in this Docker environment. The
local-only dev-access forwarder provides fixed TCP destinations while retaining
internal app/data bridges. It is not part of the AWS architecture.

This verifies packaging, wiring, initialization and the listed smoke checks. It
is not an AWS deployment, a full unit/integration test-suite run, proof of financial
correctness or a capacity test. Existing business TODOs remain. Fresh Figma access
was rate limited; see the design evidence and migration checklist in README.md.
