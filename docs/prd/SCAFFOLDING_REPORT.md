# Implementation and Verification Report — 2026-10-18

Working location: `/Users/ddj/Documents/00_code/05_Project/sinhan_fintech/pg-platform`. The existing empty structure was filled with 16 services and 5 shared libraries. The decision to keep MariaDB + PostgreSQL was confirmed with the user. No business logic, entities, business tables, business APIs or Kafka topics were created.

## Three Areas

| Area | Implementation |
| --- | --- |
| Scaffolding | Gradle Kotlin DSL with 21 modules, projectDir, Wrapper, Java21, 3 conventions, main, configuration, empty Flyway baseline, ArchitectureTest, CI |
| Boilerplate | Pure ErrorCode/ErrorType, standard error response, final Money, event contract rules, Outbox interface with post-ack status update/retry polling, Servlet/WebFlux traceId and MDC, JSON logs, health, Prometheus, OTel, OpenAPI, JDBC/Flyway, Kafka and Redis configuration, shared Testcontainers |
| Infrastructure | Standalone Compose, MariaDB, PostgreSQL, Redis, Kafka, Prometheus, Grafana, Collector; Terraform code for 6 modules, 4 VPCs, 3 peerings, FEP NAT, isolated data, 2 RDS types, Redis, MSK, private encrypted PDF S3 and Fargate |

## Defaults and Rationale

- Java21, Boot3.5.16 (the requested 3.x), Gradle8.14.4 (a Boot-supported line), JUnit5, Testcontainers1.21.4, ArchUnit1.5.1, Cloud2025.0.3, springdoc2.8.17. Libraries are managed in one place, the TOML file.
- Terraform1.16.6, AWS provider6.69.1 (lock), Seoul/dev. State uses the S3 native lockfile. The real bucket and credentials are not prepared.
- MariaDB11.8, PostgreSQL17. dev RDS: each db.t4g.micro, 20GB, Single-AZ. Redis: 1 micro. MSK: 2 t3.small. Fargate: ARM 0.25vCPU/512MiB, 1 per service. Paid toggles default to false.
- Shared web code is generated from the convention templates and shared resources. HTTP status mapping sits at each service's inbound web boundary.
- Money requires the exact currency scale. Rounding for exchange rates and fees was not arbitrarily fixed.
- Outbox is an at-least-once relay. Business storage adapters and tables are not included. Gateway JWT and rate limiting are only a skeleton, and requests other than health are blocked by default.

### R7 Detection and Limits

R7 checks where main-vpc uses Java/HTTP transport APIs. It allows only outbound adapters with known internal service names, and the negative test where an external PG adapter uses HttpClient fails. It cannot prove string URLs, reflection or all indirect communication, so the main↔mock disconnection, SGs and the FEP egress also impose limits. No financial external Kafka topics, handlers or clients were created.

## Verification

| Run | Result |
| --- | --- |
| `./gradlew build` | First full build succeeded |
| `./gradlew build -Pintegration=true --no-daemon` | Final success, 27 tests, 0 failures, 0 skipped, 16 service ArchitectureTests |
| Testcontainers | transfer db, redis and kafka components each UP, root UP |
| `python3 load-test/verify-architecture-mutations.py` | With temporary sources in the real production code, R1/R5 ArchUnit fail and R6 final inheritance fails to compile. After removal, the full build passes again |
| Compose | Data stores and observability running, transfer and account health UP, only Flyway metadata present with no business tables |
| HTTP and observability | traceId/MDC and HTTP mapping/405 tests, real 404 COMMON-HTTP-404, JVM metrics, Prometheus transfer/otel UP, Grafana database ok, Collector receives traces |
| `terraform fmt -check -recursive infra/terraform` | Passed |
| `terraform -chdir=infra/terraform/envs/dev validate` | Passed |
| `terraform plan` | Skipped because there are no AWS credentials. The STS read check returned credentials unavailable |
| `terraform apply` | Not run |
| Folder comparison | 0 missing, 0 extra service/library modules. Only convention/template and operations files were added |

Because Money is final, the compiler blocks R6 in the real source before ArchUnit runs. R6 ArchUnit subclass detection was also verified with a separately isolated non-final Money stand-in fixture. The production Money was not changed to non-final.

Evidence: verification-r1/r5/r6-expected-failure.log, verification-architecture-mutations.json, verification-gradle-build.log, verification-terraform-validate.log. A structured summary is in verification-summary.json.

## Cost and Questions

No AWS resources were created. An idle dev with everything on is about **$650~750** per month, and the idle setup with the default paid toggles off is about $0~1 per month. This assumes 730 hours and excludes taxes and large-scale traffic. The 32 endpoint AZ units, at about $303.68, are the largest fixed cost. Unit prices and calculations are in [AWS_COST.md](AWS_COST.md).

The external LLM path, Redis IAM token provider, real secret/image/backend/ACM and AWS observability address are left in [OPEN_QUESTIONS.md](OPEN_QUESTIONS.md). They were not resolved arbitrarily by adding a NAT, merging networks, Kubernetes, storing secrets or an AWS apply.

## Folder Tree — 3 directory levels, excluding build/cache

```text
pg-platform/
├─ .github/
│  └─ workflows/
├─ ai-vpc/
│  └─ ai-gateway/
│     └─ src/
├─ build-logic/
│  ├─ .kotlin/
│  │  └─ sessions/
│  └─ src/
│     └─ main/
├─ docs/
│  ├─ adr/
│  ├─ api/
│  └─ prd/
├─ fep-vpc/
│  └─ fep-adapter/
│     └─ src/
├─ gradle/
│  └─ wrapper/
├─ hooks/
├─ infra/
│  ├─ docker-compose/
│  ├─ observability/
│  └─ terraform/
│     ├─ envs/
│     └─ modules/
├─ libs/
│  ├─ arch-rules/
│  │  └─ src/
│  ├─ common-error/
│  │  └─ src/
│  ├─ common-money/
│  │  └─ src/
│  ├─ event-contracts/
│  │  └─ src/
│  └─ outbox/
│     └─ src/
├─ load-test/
├─ main-vpc/
│  ├─ channel/
│  │  └─ api-gateway/
│  ├─ core/
│  │  ├─ account/
│  │  ├─ auth/
│  │  ├─ ledger/
│  │  ├─ payment/
│  │  ├─ settlement/
│  │  └─ transfer/
│  ├─ external/
│  │  └─ swift/
│  └─ info/
│     ├─ aml/
│     └─ wealth-report/
├─ mock-vpc/
│  ├─ mock-clearing/
│  │  └─ src/
│  ├─ mock-kofiu/
│  │  └─ src/
│  ├─ mock-pg/
│  │  └─ src/
│  └─ mock-swift/
│     └─ src/
└─ skills/
```
