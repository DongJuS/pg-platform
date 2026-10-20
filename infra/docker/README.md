# Docker application stack

MVP1 재검증은 [로컬 운영 REST API](OPERATIONS_API.md)의
`POST /ops/v1/performance-runs` → `GET /ops/v1/performance-runs/{runId}`로 실행할 수 있다.
서버 시작: `python3 infra/docker/ops_api.py` (localhost:38096, 전용 운영 토큰).

This is an additive, standalone stack. It leaves `infra/docker-compose/`, Terraform,
the existing deployment workflow unchanged. MVP1 implementation status is tracked in [MVP1_COMPLETION.md](MVP1_COMPLETION.md). Project name, host ports
and named volumes differ from the old stack; existing database data is not imported.

The service registry contains all 16 applications. One multi-stage Dockerfile builds
their bootJars in a shared build stage and produces a separate non-root runtime image
for each service. Docker and Python 3.9+ are sufficient; a host JDK is not required.
The image contains the application, not the local Spring configuration or credentials.
Checked startup and packaging results are in [VERIFICATION.md](VERIFICATION.md).

## Start a small set first

Run from the repository root. Generate the env file once, outside the repository:

```sh
python3 infra/docker/init-env.py /private/tmp/sinhan-docker.env
sh infra/docker/stack.sh /private/tmp/sinhan-docker.env config --quiet
sh infra/docker/stack.sh /private/tmp/sinhan-docker.env up -d --build --wait --wait-timeout 240 api-gateway auth account ledger transfer otel-collector
python3 infra/docker/smoke.py /private/tmp/sinhan-docker.env
```

Existing env files can be upgraded without changing database passwords:

```sh
python3 infra/docker/init-env.py /private/tmp/sinhan-docker.env --upgrade
```

The `INTERNAL_SERVICE_TOKEN` is injected into Auth/Account/Ledger/Transfer and the MVP2 Payment/FEP boundary;
`AUTH_OTP_SECRET_HEX` is injected only into Auth. Keep both outside Git and images.
For an automated disposable acceptance run (valid OTP transfer and rejection cases):

```sh
python3 infra/docker/run-mvp1-e2e.py --output /private/tmp/mvp1-result.json
```

For a persistent setup, use a private directory outside the repository rather than
`/private/tmp`. `init-env.py` sets mode 0600 and refuses to overwrite existing files.
Keep the env file for as long as you keep the volumes: changing passwords in the env
file does not rotate users in an already initialized database. Init scripts run only
on empty volumes. They create ten PostgreSQL databases and two MariaDB databases,
with a separate application login and ownership/grants for each service database. The Docker-only
APP_DB_PASSWORD must be hexadecimal; the helper generates it in that format.

Selecting an app explicitly activates it without starting all other app profiles.
Database, Redis and Kafka dependencies are started automatically. The first image
build compiles all 16 jars and needs internet access for base images and Gradle
dependencies; subsequent service images reuse the build stage. This packaging step
does not run tests; continue to run the existing Gradle/CI checks.

To run everything, or add monitoring:

```sh
sh infra/docker/stack.sh /private/tmp/sinhan-docker.env --profile apps up -d --build --wait --wait-timeout 240
sh infra/docker/stack.sh /private/tmp/sinhan-docker.env --profile observability up -d
sh infra/docker/stack.sh /private/tmp/sinhan-docker.env --profile apps ps
sh infra/docker/stack.sh /private/tmp/sinhan-docker.env logs --tail 100 auth
sh infra/docker/stack.sh /private/tmp/sinhan-docker.env --profile apps --profile observability down
```

`down` retains the new stack's named volumes. Existing containers in other projects
are unaffected. Full-stack memory is substantial: 16 app limits alone total 6 GiB
with the default 384 MiB limit, in addition to infrastructure and other containers.
Start only the services needed for a flow on a machine with limited Docker memory.

## Ports and profiles

The dev-access TCP forwarder publishes all host ports on 127.0.0.1. It is a local
development access point, separate from the application Gateway. App host ports are
38080 through 38095, following
the service order in `services.tsv`; their container ports stay 8080 through 8095.
Database/cache/broker host ports are PostgreSQL 35432, MariaDB 33306, Redis 36379 and
Kafka 39092. Optional Prometheus is 39090 and Grafana is 33000. The env example lists
the infrastructure port and image-name overrides.

Compose activates `docker,skeleton` and mounts `config/application-docker.yml` through
Spring's additional configuration location. Existing default/local/aws profile behavior
is unchanged. `skeleton` is still required by the current dev-only controllers and
bean wiring. Gateway now exposes the implemented MVP1 routes, verifies JWTs and checks Redis revocation.
Internal endpoints remain blocked at Gateway. Service debug ports are local development
access only; internal operations additionally require the configured service token.

`pg.clients.<service>.base-url` configures the implemented Payment/FEP and Ledger/Transfer caller-owned
clients. Ledger queries Account for ownership/currency; Transfer queries Account and
consumes Auth OTP authorization before posting to Ledger. Payment calls Account/Ledger/FEP; FEP alone calls Mock PG. Other MVP clients remain
implementation TODOs. Outbox relay is opt-in via `compose.outbox.yaml`; it enables
Ledger, Transfer and Payment. ReportStoragePort/S3 remains outside MVP1.
Auth uses the supplied stable Mock-OTP secret. Without configured RSA PEM keys it
still creates a development JWT key on startup, so restarting Auth requires login again.
Production key and secret injection remain in the AWS readiness checklist.

Kafka is 3.9.1 to match the current MSK 3.9.x family. It has one broker locally and
creates five existing topic names with replication 1; this does not emulate MSK
durability. Redis uses upstream 7.2 locally, while Terraform selects ElastiCache 7.1:
these are different product versions, so AWS authentication and compatibility still
need their own check. The old Kafka 4.0.1/Redis 7.4 stack is preserved.

## Network boundaries and design evidence

Reference: Figma **시스템 아키텍처 구성도**, node **19:2**, and accepted ADR 0001.
Figma metadata was available in the preceding review; this change's fresh read hit
the Figma MCP call limit. Any subsequent Figma changes are **needs confirmation**.
Figma still labels EKS/Argo CD; the supplied project instructions defer Kubernetes
and the existing infrastructure uses ECS Fargate. This addition does not change that
decision. MariaDB for account/ledger and the accepted event/FEP/AI decisions remain.

Internal Compose networks model main, data, AI, FEP and mock zones. Only AI Gateway
and FEP join their respective non-internal egress networks. A separate dev-access
forwarder joins a development ingress bridge to expose fixed TCP destinations on
localhost; it is not a general HTTP/CONNECT proxy or an AWS component. Financial callers join
the FEP network, wealth-report joins the AI network, and external mocks join the
mock network. The collector/Prometheus can observe data and mock networks. These
are development bridges, not verified AWS security-group rules, VPC peering or
destination policies. Loopback debug ports are local conveniences, not public ingress.

## AWS migration checklist

- [ ] Run the same application images in ECS using immutable ECR tags/digests.
      Do not mount this local config or carry `docker,skeleton` into production.
      Finalize production bean activation for implemented features first.
- [ ] Build for the current Terraform target, linux/arm64. This Dockerfile also
      supports a buildx linux/amd64 target; select the platform that matches deployment.
- [ ] Provision per-service RDS databases/users and apply the same Flyway migrations.
      SQL schema migration is separate from copying existing data and cutting over.
- [ ] Inject DB credentials with the existing Secrets Manager mechanism; do not put
      the env file in an image, ECR, Terraform variables or Git.
- [ ] Bind the final internal REST endpoints/service discovery in each caller's
      adapter and retain Transfer -> Ledger, caller -> FEP and caller -> AI Gateway.
- [ ] Validate MSK IAM/TLS and ElastiCache IAM token generation/refresh. Redis IAM
      is still an existing implementation TODO; Docker readiness does not cover it.
- [ ] Implement/test report storage behind ReportStoragePort, including S3 download
      access and expiration; preserve the synchronous completed-report API contract.
- [ ] Omit dev-access in AWS; use the existing ALB/approved operational access.
      Connect AWS logs/traces, health checks, TLS and ingress; verify SG/peering/egress
      against Figma/ADR in AWS, then test concurrency, recovery and representative load.

Example build of one deployable image from the repository root (no AWS resource creation):

```sh
docker buildx build --platform linux/arm64 --load -f infra/docker/Dockerfile \
  --build-arg SERVICE_NAME=auth --build-arg SERVICE_PORT=8081 \
  -t sinhan_project/auth:local .
```

Docker references: [multi-stage builds](https://docs.docker.com/build/building/multi-stage/),
[Dockerfile-specific ignore files](https://docs.docker.com/build/concepts/context/#dockerignore-files),
[Compose profiles](https://docs.docker.com/compose/how-tos/profiles/).

## MVP2 synchronous payment verification

The current [Payment implementation checklist](../../main-vpc/core/payment/SKELETON.md) supersedes old statements that Payment/FEP/Mock PG are entirely skeletons. PG final result and ledger confirmation are both required before payment/refund/cancellation success. Timeout never triggers another financial request. Automatic result queries remain disabled pending clarification.

```sh
python3 infra/docker/run-mvp1-e2e.py --suite MVP2 --observability --output /private/tmp/mvp2-result.json
```

This also runs MVP1 regression, provider faults, reservations, duplicate/out-of-order signed callbacks, three-way reconciliation, and actual metrics/logs/traces/local alerts/Grafana provisioning in a fresh `pg_mvp2_*` project. All published ports are dynamically allocated on localhost; Alloy is filtered to this project. Existing stacks and volumes are preserved. This is functional acceptance, not a throughput/SLO certification.

`init-env.py --upgrade` adds the missing `MOCK_PG_TOKEN` and `PG_WEBHOOK_SECRET_HEX` without replacing existing DB credentials. Fresh stacks create `mock_pg_db`/`mock_pg_app`; existing volumes do not rerun initialization. Before an explicitly requested persistent-stack upgrade, create only that missing DB/role with the existing password (see `init/mvp2-postgres.sql`); do not delete volumes or rerun the all-database bootstrap. That upgrade and customer-stack deployment were not performed in this work.

## MVP3 전송 준비 검사

`AI_PREPARATION_WEALTH_TOKEN`은 `init-env.py`가 생성하는 호출자 전용 비밀이다. AI Gateway는 금융 공통 토큰을 인증에 사용하지 않는다. 새 env 파일을 생성하거나 기존 비밀 관리 절차로 별도 주입해야 하며 미설정은 기본 차단이다. PostgreSQL ai_gateway_db의 Flyway V2는 원문/마스킹본/복원 매핑 없는 감사 테이블을 만든다. 기존 스택에는 이번에 적용하지 않았다.

`python3 infra/docker/run-mvp1-e2e.py --suite MVP3 --observability --output /private/tmp/mvp3-result.json`은 새 프로젝트에서 MVP1/2 회귀와 AI 준비 검사를 함께 실행하고 AI 인터넷 출구를 제거한 상태로 검증한다. 기존 고객 컨테이너/데이터를 재사용하지 않는다. API와 한계는 [AI 구현 체크리스트](../../ai-vpc/ai-gateway/SKELETON.md)를 따른다. 외부 LLM·API 키·AWS/NAT 활성화는 포함하지 않는다.

## MVP5 isolated SWIFT acceptance

`python3 infra/docker/run-mvp1-e2e.py --suite MVP5 --output /private/tmp/mvp5-result.json` runs MVP1–MVP4 regressions and the USD/no-FX/zero-fee remittance flow. It creates fresh service-owned databases, Kafka topics, caller credentials, a durable local Mock and private log directories. A test-only internal proxy disconnects an actual committed Ledger response for the recovery check. It has no host port and is never added to the canonical customer stack.

The runner alone enables `swift-test`, explicit Mock retransmission and a synthetic administrator. Default operating activation/retransmission remain disabled. Containers, volumes and private test data are cleaned up; original customer containers are checked for preservation. `--swift-only` is a diagnostic shortcut and must not be reported as complete regression acceptance. See [MVP5 contract and limitations](../../main-vpc/external/swift/SKELETON.md).
