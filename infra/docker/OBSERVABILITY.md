# Observability override (alertmanager / tempo / loki / alloy / localstack / kafka-exporter)

`compose.observability.yaml` is an opt-in Compose override for the additive
`compose.yaml` stack. The base files stay untouched: applying or dropping the
override is the only switch. It adds Alertmanager, Tempo, Loki, a Grafana Alloy
container-log collector, a LocalStack S3 substitute and a Kafka exporter, wires
OTLP log export from all 16 app services into Loki, and re-mounts
Prometheus/Grafana/otel-collector configuration so alert rules, dashboards,
the logs pipeline and trace/log datasources become active. All shared
configuration files live in `infra/observability/` and are only mounted, never
modified here.

## Usage

Run from the repository root. `stack.sh` forwards extra arguments to
`docker compose`, so pass the override as an additional `-f` right after the
base file (merge order matters — the override must come last):

```sh
ENV=/private/tmp/sinhan-docker.env   # init-env.py output; see README.md

# Syntax check: base alone, then base + override
sh infra/docker/stack.sh $ENV config --quiet
sh infra/docker/stack.sh $ENV -f infra/docker/compose.observability.yaml config --quiet

# Start observability on top of whatever app subset you run
sh infra/docker/stack.sh $ENV -f infra/docker/compose.observability.yaml \
  --profile observability up -d --wait --wait-timeout 240 \
  prometheus grafana alertmanager tempo loki alloy kafka-exporter otel-collector

# Optional S3 substitute (separate profile, not part of observability)
sh infra/docker/stack.sh $ENV -f infra/docker/compose.observability.yaml \
  --profile localstack up -d --wait localstack localstack-init

# Stop only what this project started; named volumes are retained (no -v!)
sh infra/docker/stack.sh $ENV -f infra/docker/compose.observability.yaml \
  --profile observability --profile localstack down
```

Explicit service names activate their profiles automatically; the
`--profile` flags above are only needed for bare `up`/`down`. Without the
`-f` argument the stack behaves exactly as documented in README.md.

## Services and ports

| Service      | Host port (127.0.0.1) | Container port | Image                      | Profile        |
|--------------|-----------------------|----------------|----------------------------|----------------|
| Prometheus   | 39090 (via dev-access)| 9090           | prom/prometheus:v3.5.0     | observability  |
| Grafana      | 33000 (via dev-access)| 3000           | grafana/grafana:12.1.1     | observability  |
| Alertmanager | 39093 (direct)        | 9093           | prom/alertmanager:v0.34.1  | observability  |
| Tempo        | 33200 (direct)        | 3200           | grafana/tempo:3.0.4        | observability  |
| Loki         | 33100 (direct)        | 3100           | grafana/loki:3.6.18        | observability  |
| Alloy        | none (logs push on `data`) | 12345 (not published) | grafana/alloy:v1.11.3 (digest-pinned) | observability |
| kafka-exporter | none (scraped on `data`) | 9308       | danielqsj/kafka-exporter:v1.10.0 (digest-pinned) | observability |
| LocalStack   | 34566 (direct)        | 4566           | localstack/localstack:3.8.1| localstack     |

Direct port publishing deviates from the base stack's dev-access forwarder
pattern because `config/haproxy.cfg` has no free listener slots and must not
be modified. Publishing only works because the new services additionally join
`obs-ingress`, a non-internal loopback bridge added by this override: a
container attached solely to `internal: true` networks has no gateway and
Docker silently skips port publishing (verified 2026-10-18). The VPC-zone
networks (`main`, `data`, `mock`, ...) keep their original semantics; the new
services sit on `data` like the collector and Prometheus, per the README
principle that observability observes the data/mock zones.

LocalStack runs with `SERVICES=s3` and `PERSISTENCE=1`. The one-shot
`localstack-init` service (amazon/aws-cli) creates bucket
`pg-platform-local-reports` idempotently (`head-bucket` before `mb`).
Endpoint for local development: `http://localhost:34566`, credentials
`test`/`test`, region `ap-northeast-2`.

## What the override changes in existing services

- **prometheus**: the base mount `config/prometheus.yml` is replaced by
  `config/prometheus.observability.yml` (identical scrape targets, plus
  `rule_files`, an `alerting:` section pointing at `alertmanager:9093`, and
  self-monitoring jobs for prometheus/alertmanager/tempo/loki/otel-collector).
  `infra/observability/prometheus-alerts.yml` is mounted at
  `/etc/prometheus/rules/alerts.yml`. Compose merges volumes by target path,
  so listing the same target with a new source replaces the base mount.
- **grafana**: adds `infra/observability/dashboards/` as the dashboard
  provisioning directory. The base service already mounts
  `infra/observability/grafana-datasources.yml`, which includes the Tempo and
  Loki datasources — no datasource change is needed.
- **otel-collector**: the base mount `infra/observability/otel-collector.yml` is
  replaced by `config/otel-collector.observability.yml` (identical receivers,
  processors and traces/metrics pipelines, plus an `otlphttp/loki` exporter and
  a `logs` pipeline targeting Loki's native OTLP ingest
  `http://loki:3100/otlp/v1/logs`; the deprecated `loki` exporter is not used).
  The logs pipeline deliberately lives only in this variant: in the shared file
  it would make the base-only collector retry forever against a nonexistent
  loki. Compose merges volumes by target path, so this mount replaces the base
  one. No `depends_on` on tempo/loki is added on purpose: the collector has no
  profile, so depending on the profiled services would make the combined config
  invalid unless the observability profile is enabled; both exporters retry
  until their backends accept connections.
- **app services (all 16)**: each gets
  `MANAGEMENT_OTLP_LOGGING_ENDPOINT=http://otel-collector:4318/v1/logs` merged
  into its environment (the base `x-app` anchor cannot be shared across files,
  so the override lists one block per service). This is Spring Boot 3.5's OTLP
  log export (`management.otlp.logging.endpoint`) and requires no Java change.
  Without the override the variable is absent and log export stays off — base
  behavior is unchanged.
- **kafka-exporter** (new): scrapes broker-side consumer-group offsets from
  `kafka:19092` (`--kafka.version=3.9.1`) and is scraped by Prometheus via the
  new `kafka-exporter` job in `config/prometheus.observability.yml`. No host
  port is published. Consumer groups that never committed offsets produce no
  lag series, so with consumers still unimplemented the guaranteed outcome is
  the scrape path (target UP), not data presence. Relationship to the app-side
  `kafka_consumer_fetch_manager_records_lag_max` and the alert-rule decision:
  `infra/observability/METRICS.md` §3.
- **alloy** (new, WO-D): collects container logs via the Docker **daemon API**
  (`loki.source.docker` over the read-only-mounted `/var/run/docker.sock`), not
  log files, so it works on macOS Docker Desktop — the promtail docker-SD path
  failed locally because Docker Desktop keeps `/var/lib/docker/containers`
  inside the Linux VM. `infra/observability/alloy.alloy` filters discovery to
  this project only (`com.docker.compose.project=sinhan_project_docker`,
  applied at the Docker API level, so other projects' containers are never
  even listed as targets) and pushes to Loki with `service` (compose service
  name) and `stream` (stdout/stderr) labels. Alloy is **logs-only**: metrics
  and traces keep their existing paths (Prometheus scrape / OTLP push) and are
  not duplicated through Alloy. The docker.sock mount grants
  host-root-equivalent power even read-only — this path is local-development
  only; on Linux/AWS use a least-privilege collector instead. The healthcheck
  is a `--version` liveness probe (the image has a shell but no wget/curl, so
  an in-container HTTP probe on `12345/-/ready` cannot run); true readiness is
  a successful Loki query for a real log line.

New named volumes (`alertmanager-data`, `tempo-data`, `loki-data`,
`localstack-data`) are prefixed by the project (`sinhan_project_docker_*`)
and do not collide with the old stack's volumes.

## Alerts and runbook

Nine alert rules in three groups load from
`infra/observability/prometheus-alerts.yml`:

- **platform-availability**: `Watchdog` (always firing — pipeline check),
  `ServiceDown`
- **http-service**: `HighHttp5xxErrorRate`, `HighLatencyP95`, `HighLatencyP99`
- **messaging**: `KafkaConsumerLagHigh`, `OutboxBacklogHigh`,
  `OutboxOldestEventStale`, `ReconciliationMismatch`

The Outbox/Reconciliation rules are placeholders: their metrics are not
implemented yet, so they never fire until the metrics exist. Alertmanager
uses a local-only receiver — alerts are visible in the UI
(`http://localhost:39093/#/alerts`) and API only; Slack/PagerDuty integration
is a commented block for the AWS migration. Response procedures:
`infra/observability/RUNBOOK.md` (each rule's `runbook_url` annotation points
into it). Threshold rationale: `infra/observability/METRICS.md`.

## Verification record (2026-10-18)

- `config -q`: base alone OK; base + override OK; with both profiles OK.
- Started prometheus, alertmanager, tempo, loki, localstack, localstack-init,
  otel-collector (plus dev-access dependency), then:
  - readiness: prometheus `/-/ready` 200 (via 39090), alertmanager 200
    (39093), tempo `/ready` 200 (33200), loki `/ready` 200 (33100),
    localstack `/_localstack/health` 200 (34566).
  - `GET :39090/api/v1/rules` → 3 groups / 9 rules loaded.
  - `GET :39090/api/v1/alertmanagers` → alertmanager active, none dropped;
    `Watchdog` and `ServiceDown` (apps not running) visible in
    `GET :39093/api/v2/alerts` — Prometheus→Alertmanager path verified.
  - Trace path verified end-to-end: a probe span POSTed to
    `otel-collector:4318/v1/traces` inside the `data` network was retrievable
    from Tempo by trace ID (`GET :33200/api/traces/<id>`).
  - localstack-init log: `make_bucket: pg-platform-local-reports`;
    `awslocal s3 ls` lists the bucket. Idempotency relies on `head-bucket`.
- Afterwards only these services were brought down (`down`, no `-v`);
  volumes retained; other projects' containers untouched.

## Verification record (2026-10-18, WO-3: kafka-exporter + OTLP logs)

- `config -q` (all profiles enabled): base alone OK; base + override OK.
  The merged override config carries `MANAGEMENT_OTLP_LOGGING_ENDPOINT` on all
  16 app services and the swapped otel-collector mount; the base-only config
  carries neither.
- Minimal runtime check (kafka, prometheus, kafka-exporter, loki, tempo,
  otel-collector only; apps not built/started):
  - `kafka-exporter` target `up` at `:39090/api/v1/targets`;
    `kafka_brokers{job="kafka-exporter"} = 1`. `kafka_consumergroup_lag` is an
    empty vector — expected: no consumer group has ever committed offsets.
    Completion criterion (scrape path UP) met.
  - loki `/ready` → `ready` (33100); OTLP ingest probe
    `POST :33100/otlp/v1/logs` → 204 and the probe record was queryable via
    `{service_name="wo3-probe"}` — Loki's native OTLP endpoint works.
  - otel-collector started with the override config (`Everything is ready`);
    logs pipeline loaded. End-to-end **app** log ingestion remains to be
    verified when app containers run (full-stack smoke wave).
- Fixed two latent healthcheck bugs found during verification: grafana/loki
  and grafana/tempo images are distroless (no `/bin/sh`, no `wget`), so the
  previous `CMD-SHELL wget` healthchecks could never run (loki reported
  `unhealthy` under `--wait`). Both now use a `-version` liveness probe; true
  readiness stays external (`curl :33100/ready`, `curl :33200/ready`,
  `up{job=...}` in Prometheus).
- Afterwards the stack was brought down (`down`, no `-v`); all named volumes
  retained; the unrelated `sinhan_project` project containers untouched.

## Verification record (2026-10-18, WO-D: Alloy container-log collection)

- `config -q`: base alone OK; base + override OK; with both profiles OK. The
  merged config carries the `alloy` service with `/var/run/docker.sock`
  mounted **read-only** (verified in `config --no-interpolate` output).
- `infra/observability/alloy.alloy` passes `alloy fmt` and `alloy validate`
  (grafana/alloy:v1.11.3 container).
- Minimal runtime check (`loki alloy otel-collector` only; apps not
  built/started):
  - loki `/ready` → `ready`; alloy container healthy (liveness probe).
  - **Real daemon-read logs in Loki** (not a synthetic probe):
    `GET :33100/loki/api/v1/query_range` with `{service=~".+"}` returned
    streams for `service="loki"`, `service="otel-collector"` and
    `service="alloy"` with real startup/connection log lines. Note: the
    instant-query endpoint rejects log queries — use `query_range`.
  - `stream` label verified: `{service="otel-collector", stream="stderr"}`
    returns entries. Caveat found during verification: Alloy's
    `loki.source.docker` does **not** export the stream label automatically —
    it sets `__meta_docker_container_log_stream` per entry, so the
    `relabel_rules` argument must map it to `stream` (done in alloy.alloy).
  - Project filter verified: no log entries from the concurrently running
    `sinhan_project` (other project) containers — `{service=~"mariadb|sinhan_project"}`
    returns zero streams.
  - No errors in the alloy container log during collection.
- Afterwards the stack was brought down (`down`, no `-v`); all named volumes
  retained (`docker volume ls` shows all `sinhan_project_docker_*` volumes);
  the unrelated `sinhan_project` containers untouched.
- Not yet covered: stdout-labeled entries from actual app containers (the
  Spring Boot services were not running; same code path as stderr, verified
  when the app stack runs); Grafana Explore browsing of the streams.

## Known gaps / needs confirmation

- **Local log collection path: resolved (WO-D, 2026-10-18).** Container logs
  are collected by Grafana Alloy via the Docker daemon API
  (`loki.source.docker` → `loki.write` → `http://loki:3100`), which works on
  macOS Docker Desktop. This **corrects the earlier record** that local
  collection was impossible: what fails on macOS is only promtail's docker
  service discovery, which reads log **files** that live inside the Docker
  Desktop VM; `infra/observability/promtail.yml` remains a Linux/AWS reference
  only. The Alloy docker.sock mount is read-only but still
  host-root-equivalent — local development only (WO-D constraint).
  The WO-3 OTLP logs wiring (16× `MANAGEMENT_OTLP_LOGGING_ENDPOINT` +
  otel-collector logs pipeline) is **kept, not removed**: it activates when
  Backend later adopts an OTLP log appender (Spring Boot 3.5
  `management.otlp.logging.endpoint`) — until then it stays idle and Alloy is
  the active local path. Both paths land in the same Loki, so Grafana Explore
  queries work regardless of which path delivered the entry.
- Tempo/Loki retention is an initial 168h; financial retention policy is
  undecided — do not treat as an operating standard.
- **Kafka consumer lag: resolved (WO-3, 2026-10-18).** The app metric
  `kafka_consumer_fetch_manager_records_lag_max` auto-exposes via Boot 3.2+
  Micrometer registration once `@KafkaListener` consumers actually exist
  (currently skeletons — no series, expected). kafka-exporter now provides the
  broker-side path (`kafka_consumergroup_lag`). Decision recorded in
  `infra/observability/METRICS.md` §3: the `KafkaConsumerLagHigh` rule stays on
  the app metric; exporter metrics are for dashboards.
- Grafana dashboard provisioning mounts the directory read-only; existing
  stale `grafana-data` volume behavior is out of scope (per project rules,
  volumes are never deleted).
- LocalStack emulates S3 API only; it does not validate the AWS-side
  versioning/SSE/TLS-enforcement bucket policy from the Terraform design.
