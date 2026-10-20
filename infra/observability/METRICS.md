# 업무 메트릭 명명·카디널리티 규약 (MVP2-F12)

> Outbox 미발행 건수·최장 나이·DB 수집 성공 지표는 구현됐다.
> 서비스 로컬 OutboxStore/JdbcTemplate/MeterRegistry가 있을 때 자동 등록되며,
> relay를 꺼도 적체를 관측한다. Payment 대사 불일치 Counter와 FEP 대외 전송 Histogram도 구현됐다. 분개·이체의 업무 Counter는 아직 구현 전이다.

## 1. 명명 규칙

- 접두사: 모든 업무 메트릭은 `pg_`로 시작한다. 표준 Actuator 메트릭과 충돌하지 않게 한다.
- 형식: `pg_<서비스>_<대상>_<행위>_<단위>` (Prometheus 관례: 소문자 snake_case).
  - 서비스 세그먼트는 서비스 디렉터리 이름 그대로 사용(하이픈은 언더스코어로):
    `api_gateway`, `auth`, `account`, `ledger`, `transfer`, `payment`, `settlement`,
    `swift`, `wealth_report`, `aml`, `ai_gateway`, `fep_adapter`.
  - Counter는 `_total` 접미사, 시간은 `_seconds`, 크기는 단위 접미사(`_bytes`, `_won` 등)를 붙인다.
  - Micrometer에서 `pg.transfer.created`로 등록하면 Prometheus 노출 시 `pg_transfer_created_total`이 된다.

### 메트릭 목록(Outbox·Payment 대사·FEP 전송 구현, 분개/이체 Counter는 예정)

| 메트릭 | 타입 | 서비스 | 의미 | 사용처 |
|---|---|---|---|---|
| `pg_transfer_created_total` | Counter | transfer | 이체 생성 건수 | 처리량 |
| `pg_ledger_journal_posted_total` | Counter | ledger | 분개 완료 건수(`ledger.posted`와 1:1이어야 함) | 처리량·정합성 |
| `pg_outbox_unpublished_events` | Gauge | 공통(libs/outbox) | 미발행 outbox_events 건수 | `OutboxBacklogHigh` |
| `pg_outbox_oldest_unpublished_age_seconds` | Gauge | 공통(libs/outbox) | 최장 미발행 이벤트 나이 | `OutboxOldestEventStale` |
| `pg_outbox_metrics_sample_success` | Gauge | 공통(libs/outbox) | 최근 DB 표본 성공 1 / 실패·초기 미수집 0 | `OutboxMetricsUnavailable` |
| `pg_payment_reconciliation_mismatch_total` | Counter | payment | 대사 불일치 건수 | `ReconciliationMismatch` |
| `pg_fep_adapter_send_duration_seconds` | Histogram | fep-adapter | 대외 기관 전송 지연 | 지연 관찰 |

- 공통 라이브러리(libs/outbox 등)가 노출하는 메트릭은 서비스 세그먼트를 생략하고
  스크레이프 시 붙는 `service` 라벨로 출처를 구분한다(위 `pg_outbox_*`가 해당).

## 2. 라벨 카디널리티 제한

- **허용 라벨**(저카디널리티, 열거 가능): `service`(16개 고정), `topic`(Kafka 토픽명,
  현재 8개: `ledger.posted`, `transfer.completed`, `transfer.outbound`, `fep.ack`,
  `payment.approved`, `payment.refunded`, `payment.cancelled`, `recon.mismatch`), `result`/`status`/`error_type`(열거형 5개 이내), `channel`(열거형).
- **금지 라벨**(고카디널리티 — 절대 라벨로 두지 않는다): 계좌번호, 거래 ID, 멱등성 키,
  사용자 ID, 전문 번호, 카드번호 등 식별자 전부. 이런 값은 trace/로그로만 추적한다.
- 하나의 메트릭 계열(series)당 라벨 조합은 **100개 이하**를 원칙으로 한다.
  초과가 예상되면 라벨을 빼고 Counter를 분리한다.
- Actuator의 `http_server_requests_seconds`는 `uri` 라벨이 패턴화된 경우에만 허용한다.
  경로 변수가 그대로 들어가는 엔드포인트는 반드시 패턴 매핑을 확인한다.

## 3. 인프라 메트릭 현황(구현 없이 사용 가능)

- `http_server_requests_seconds_count/bucket/sum`: 오류율·지연 alert와 개요 대시보드의 근거.
- `up`: `ServiceDown` alert의 근거.
- Kafka consumer lag — **두 경로가 공존하며 역할이 다르다(모순 아님, WO-3 확정)**:
  - `kafka_consumer_fetch_manager_records_lag_max` (앱 메트릭, 클라이언트 측):
    Spring Boot 3.2+는 MeterRegistry가 존재하면 Kafka producer/consumer Micrometer
    listener를 자동 등록하므로, `@KafkaListener` 컨슈머가 실제 생성되는 순간 자동 노출된다.
    **MVP1 Transfer의 ledger.posted 소비자는 구현됐고 relay는 opt-in으로 활성화한다.**
    Payment 등 나머지 소비자는 스켈레톤이다. 실제 인스턴스별 시리즈는 활성 상태에 따라 확인한다.
  - `kafka_consumergroup_lag` 등 (kafka-exporter, 브로커 측):
    `compose.observability.yaml`의 `kafka-exporter` 서비스(observability 프로필)가
    브로커의 커밋된 consumer group offset을 스크랩한다. 앱 계측과 무관하게 동작하지만
    **consumer group이 offset을 커밋한 적이 없으면 시리즈가 비어 있다.**
    MVP1 분리 Docker 검증에서는 실제 소비·offset 커밋·lag 0을 확인한다.
  - 결정(2026-10-18): alert 룰 `KafkaConsumerLagHigh`는 앱 메트릭
    (`kafka_consumer_fetch_manager_records_lag_max`) 기준을 **유지**한다
    (인스턴스·client_id 단위 탐지용). exporter 메트릭은 그룹·토픽 단위 개요
    대시보드용으로 사용한다. 양쪽 모두 컨슈머 구현 전까지는 발화/데이터 없음이 정상.

## 4. 보존·샘플링

- Outbox DB 표본은 기본 15초마다 `published_at IS NULL`의 건수와 가장 오래된 생성 시각을 함께 읽는다.
  lease 중이거나 재시도 대기인 행도 포함한다. HTTP 스크레이프에서는 DB를 읽지 않는다.
  빈 집합은 건수/나이 0, DB 오류는 두 값 NaN과 sample_success=0이다.
  `outbox.metrics.enabled=false`로 비활성화하거나 `outbox.metrics.poll-delay-ms`로 주기를 조정한다.
  Figma 근거: `9:220` Alertmanager, `9:223` Runbook. 금융 보존·SLO 정책을 새로 확정하지 않는다.

- Prometheus 스크레이프 15s(기존 `prometheus.yml` 유지). 로컬 보존은 Prometheus 기본(15d).
- trace 샘플링은 `platform-defaults.yml`의 `TRACE_SAMPLING_PROBABILITY`(기본 0.1)를 따른다.
  로컬에서만 1.0으로 올려 디버깅할 수 있으나 기본값은 바꾸지 않는다.
- Loki/Tempo 보존은 각 설정 파일의 168h를 초기값으로 둔다. 금융 보존 정책은 미정 — 임의 확정 금지.

## MVP3 전송 준비

AI Gateway는 기존 `/actuator/prometheus`와 OTel/구조화 로그 경로를 사용한다. `pg_ai_preparation_decisions_total{decision,reason}`은 감사 저장을 마친 준비/차단/검사 오류 판정이다. 두 라벨은 enum 고정 집합이며 서비스 이름·사용자/요청 ID·프롬프트·마스킹 값·비밀·유형별 건수를 넣지 않는다. 인증/JSON/transport 거절과 감사 저장 실패는 기존 HTTP 상태 메트릭으로 관측한다. 업무 로그도 `eventType=ai_preparation decision=... reason=...`만 기록한다. 모델 토큰 사용량/쿼터 통계는 구현하지 않았다. 분리 MVP3 E2E가 실제 Prometheus 수집을 검증한다.
