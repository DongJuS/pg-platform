# FULL_STACK_SMOKE — 그룹 기동 전체 스모크 (WO-4)

- 일자: 2026-10-18
- 대상: canonical 스택 `infra/docker/compose.yaml` (+ override `compose.observability.yaml`), 프로젝트 `sinhan_project_docker`, env `/private/tmp/sinhan-docker.env`
- 호스트: macOS (Docker Desktop), **Docker VM 총 메모리 8,318,844,928 B ≈ 7.75 GiB**, 15 CPU
- 이미지: `sinhan_project/<svc>:local` (멀티스테이지 `infra/docker/Dockerfile`, 사전 빌드됨)

> VERIFICATION.md는 canonical 소유물이므로 본 문서를 별도로 작성한다.

---

## 1. 왜 그룹 기동인가 — 메모리 산식

compose `mem_limit` 상한 합계 (모두 동시 기동 가정):

| 구분 | 항목 | 합계 |
| --- | --- | --- |
| 앱 16개 | 16 × 384m | 6144 MiB |
| 데이터 인프라 | postgres 384 + mariadb 384 + redis 96 + kafka 768 + kafka-init 192 + kafka-volume-init 32 + otel-collector 192 + dev-access 64 | 2112 MiB |
| observability override | prometheus 192 + grafana 256 + alertmanager 96 + tempo 256 + loki 256 + kafka-exporter 96 | 1152 MiB |
| localstack 프로필 | localstack 512 + localstack-init 96 | 608 MiB |
| **합계** | | **10016 MiB ≈ 9.78 GiB** |

9.78 GiB > VM 7.75 GiB → **전체 동시 기동 불가** (VERIFICATION.md와 동일 결론).
`--profile apps` 전체 + override 동시 기동은 시도하지 않았다 (비검증 주장 방지).
전체 동시 기동이 필요하면 Docker Desktop VM 메모리 증설이 선행돼야 한다 (호스트 설정 변경 — 사용자 결정 사항, 이번 범위 아님).

## 2. 그룹 정의와 검증 명령

공통: `sh infra/docker/stack.sh /private/tmp/sinhan-docker.env [-f infra/docker/compose.observability.yaml --profile observability ...] up -d --wait --wait-timeout 240 <서비스들>` → `python3 infra/docker/smoke.py /private/tmp/sinhan-docker.env <서비스들>` → prometheus `http://localhost:39090/api/v1/targets` → `docker stats --no-stream` → 해당 그룹 앱만 `rm -sf`. 볼륨 유지(`down -v` 미사용).

| 그룹 | 서비스 | 추가 프로필 |
| --- | --- | --- |
| G1 channel/core | api-gateway auth account ledger transfer | — |
| G2 금융·대외·mock | payment settlement swift + mock-pg mock-swift mock-kofiu mock-clearing | — |
| G3 info·ai | wealth-report aml ai-gateway fep-adapter | override 포함 (OTLP 로그 env 확인용) |
| G4 observability + localstack | prometheus grafana otel-collector alertmanager tempo loki kafka-exporter + localstack localstack-init | `--profile observability --profile localstack` |

인프라(postgres/mariadb/redis/kafka/kafka-init/dev-access/otel-collector)는 depends_on으로 자동 포함.

## 3. 결과

### G1 — PASS (기동 17.8s, 전원 healthy)

- `smoke.py api-gateway auth account ledger transfer` → 5/5 PASS (health UP)
- observability 서브셋(prometheus grafana otel-collector alertmanager tempo loki kafka-exporter)과 **동시 기동 PASS** (7.7s 추가)
- prometheus targets: auth / account / ledger / transfer = **up**
  (api-gateway는 `config/prometheus*.yml`의 services job에 원래 스크랩 대상으로 없음 — smoke.py로 기동 확인)
- G1+obs `docker stats --no-stream` 스냅샷 (실사용/상한):

| 컨테이너 | MemUsage | 컨테이너 | MemUsage |
| --- | --- | --- | --- |
| api-gateway | 250.3MiB / 384MiB | prometheus | 38.0MiB / 192MiB |
| auth | 249.5MiB / 384MiB | grafana | 75.3MiB / 256MiB |
| account | 261.9MiB / 384MiB | alertmanager | 16.1MiB / 96MiB |
| ledger | 264.9MiB / 384MiB | tempo | 59.0MiB / 256MiB |
| transfer | 274.4MiB / 384MiB | loki | 47.4MiB / 256MiB |
| postgres | 55.5MiB / 384MiB | kafka-exporter | 8.6MiB / 96MiB |
| mariadb | 98.0MiB / 384MiB | otel-collector | 69.0MiB / 192MiB |
| redis | 15.1MiB / 96MiB | kafka | 307.3MiB / 768MiB |
| dev-access | 13.6MiB / 64MiB | **실사용 합계** | **≈ 2.1 GiB** (상한 합계 ≈ 5.1 GiB) |

### G2 — PASS (기동 12.3s, 전원 healthy)

- `smoke.py payment settlement swift mock-pg mock-swift mock-kofiu mock-clearing` → 7/7 PASS
- observability 서브셋 유지 상태로 **동시 기동 PASS**
- prometheus targets: payment / settlement / swift / mock-pg / mock-swift / mock-kofiu / mock-clearing = **up**
- G2+obs stats 스냅샷: payment 230.6 / settlement 233.7 / swift 233.5 / mocks 4개 159~161 MiB(각 /384MiB), 인프라·관측은 G1과 동수준. **실사용 합계 ≈ 2.2 GiB**

### G3 — PASS (기동 11.9s, 전원 healthy)

- `smoke.py wealth-report aml ai-gateway fep-adapter` → 4/4 PASS
- override 포함으로 기동 → 컨테이너 내 `MANAGEMENT_OTLP_LOGGING_ENDPOINT=http://otel-collector:4318/v1/logs` 설정 확인 (printenv)
- prometheus targets: wealth-report / aml / ai-gateway / fep-adapter = **up**
- G3+obs+localstack stats 스냅샷: wealth-report 251.0 / aml 248.0 / ai-gateway 241.7 / fep-adapter 245.3 MiB(각 /384MiB), localstack 171.3MiB / 512MiB, tempo 196.1MiB / 256MiB. **실사용 합계 ≈ 2.3 GiB**

### G4 observability + localstack — PASS (조건부 1건, §4)

- localstack healthy, `localstack-init` 완료, S3 버킷 `pg-platform-local-reports` 생성 확인 (`awslocal s3 ls`)
- prometheus `/api/v1/rules`: **룰 9개 전부 로드** — platform-availability(Watchdog, ServiceDown), http-service(HighHttp5xxErrorRate, HighLatencyP95, HighLatencyP99), messaging(KafkaConsumerLagHigh, OutboxBacklogHigh, OutboxOldestEventStale, ReconciliationMismatch)
  - Watchdog firing = 정상. ServiceDown firing = 그룹 교체로 down된 앱 타깃이 있어 firing — 그룹 기동 방식의 예상 결과
- kafka-exporter target = **up** (`http://kafka-exporter:9308/metrics`, lastError 없음)
- `kafka_consumergroup_lag` 시리즈 = 0건 — consumer가 skeleton 상태라 commit된 offset이 없으므로 **예상된 빈 결과** (경로 확보가 완료 기준)

## 4. Loki 로그 end-to-end — FAIL (원인 확정)

- 기대: 앱 ≥1개 기동 상태에서 otel-collector 경유 앱 로그가 Loki에 유입, `{service_name=~".+"}` 쿼리로 조회
- 실측: Loki `service_name` 라벨 값은 `wo3-probe` 1건뿐 (WO-3 작업자의 합성 probe — collector→Loki 경로 자체는 증명됨). G3 앱 4개의 로그는 유입되지 않음
- 중간 확인: G3 컨테이너에 `MANAGEMENT_OTLP_LOGGING_ENDPOINT` 설정됨, 앱은 logback-classic 1.5.34 + JSON console 출력으로 로그를 정상 생산, 앱의 OTLP **traces**는 collector debug exporter에서 계속 수신됨 (앱→collector 네트워크 정상)
- **원인**: Spring Boot 3.5.16의 `management.otlp.logging.endpoint`는 `OtlpHttpLogRecordExporter` 빈만 자동 구성한다. 로그 이벤트를 OTel SDK로 넘기는 **Logback appender(`io.opentelemetry.instrumentation:opentelemetry-logback-appender-1.0`의 `OpenTelemetryAppender`)는 Spring Boot에 포함되지 않으며**, logback 설정 + `OpenTelemetryAppender.install(openTelemetry)` 초기화 코드가 필요하다 (공식 문서 명시). 앱 jar(BOOT-INF/lib)에 해당 appender가 없음을 실物 확인 → 클래스패스/코드 수정 없이는 앱 로그 OTLP export 불가
- 결론: infra 경로(apps→otel-collector logs pipeline→Loki `/otlp/v1/logs`)는 배선돼 있으나, **앱 측 appender 추가는 Java 소스·의존성 변경이 필요하여 이번 범위(수정 금지) 밖 — 확인 필요**. 해소 방안: `opentelemetry-logback-appender-1.0` 의존성 + logback-spring.xml appender + initializer 추가, 또는 AWS 이전 시 promtail/Alloy 기반 수집으로 전환

## 5. 종합

| 항목 | 결과 |
| --- | --- |
| G1 smoke + obs 동시 기동 | PASS |
| G2 smoke + obs 동시 기동 | PASS |
| G3 smoke + obs 동시 기동 | PASS |
| G4 (룰 9개, kafka-exporter UP, localstack+버킷) | PASS |
| 앱 로그 → Loki end-to-end | **FAIL — 원인 확정(§4), 코드 변경 필요** |
| 전체 동시 기동 | 미시도 (메모리 산식상 불가, §1) |

- 종료: `stack.sh ... down` (볼륨 유지, `-v` 미사용)
- 다른 compose 프로젝트(`sinhan_project` 등) 컨테이너는 접촉하지 않음
