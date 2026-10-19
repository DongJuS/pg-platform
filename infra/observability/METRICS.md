# 업무 메트릭 명명·카디널리티 규약 (MVP2-F12)

> 상태: 규약 문서. **현재 코드에는 업무 메트릭이 하나도 구현돼 있지 않다.**
> 스켈레톤 서비스가 노출하는 것은 Spring Actuator의 표준 메트릭
> (`http_server_requests_seconds_*`, `jvm_*`, `process_*` 등)뿐이다.
> 이 문서는 업무 메트릭을 **구현할 때 따라야 할 규약**이며, `prometheus-alerts.yml`의
> 일부 룰(Outbox·대사)은 이 규약의 메트릭이 구현되기 전까지 평가되지 않는다.

## 1. 명명 규칙

- 접두사: 모든 업무 메트릭은 `pg_`로 시작한다. 표준 Actuator 메트릭과 충돌하지 않게 한다.
- 형식: `pg_<서비스>_<대상>_<행위>_<단위>` (Prometheus 관례: 소문자 snake_case).
  - 서비스 세그먼트는 서비스 디렉터리 이름 그대로 사용(하이픈은 언더스코어로):
    `api_gateway`, `auth`, `account`, `ledger`, `transfer`, `payment`, `settlement`,
    `swift`, `wealth_report`, `aml`, `ai_gateway`, `fep_adapter`.
  - Counter는 `_total` 접미사, 시간은 `_seconds`, 크기는 단위 접미사(`_bytes`, `_won` 등)를 붙인다.
  - Micrometer에서 `pg.transfer.created`로 등록하면 Prometheus 노출 시 `pg_transfer_created_total`이 된다.

### 예시(구현 예정 목록 — 아직 미구현)

| 메트릭 | 타입 | 서비스 | 의미 | 사용처 |
|---|---|---|---|---|
| `pg_transfer_created_total` | Counter | transfer | 이체 생성 건수 | 처리량 |
| `pg_ledger_journal_posted_total` | Counter | ledger | 분개 완료 건수(`ledger.posted`와 1:1이어야 함) | 처리량·정합성 |
| `pg_outbox_unpublished_events` | Gauge | 공통(libs/outbox) | 미발행 outbox_events 건수 | `OutboxBacklogHigh` |
| `pg_outbox_oldest_unpublished_age_seconds` | Gauge | 공통(libs/outbox) | 최장 미발행 이벤트 나이 | `OutboxOldestEventStale` |
| `pg_payment_reconciliation_mismatch_total` | Counter | payment | 대사 불일치 건수 | `ReconciliationMismatch`(자리 표시) |
| `pg_fep_adapter_send_duration_seconds` | Histogram | fep-adapter | 대외 기관 전송 지연 | 지연 관찰 |

- 공통 라이브러리(libs/outbox 등)가 노출하는 메트릭은 서비스 세그먼트를 생략하고
  스크레이프 시 붙는 `service` 라벨로 출처를 구분한다(위 `pg_outbox_*`가 해당).

## 2. 라벨 카디널리티 제한

- **허용 라벨**(저카디널리티, 열거 가능): `service`(16개 고정), `topic`(Kafka 토픽명,
  현재 5개: `ledger.posted`, `transfer.completed`, `transfer.outbound`, `fep.ack`,
  `payment.approved`), `result`/`status`/`error_type`(열거형 5개 이내), `channel`(열거형).
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
    **현재는 모든 컨슈머가 스켈레톤(TODO)이고 outbox relay도 비활성이라 시리즈가 없다.**
    컨슈머 구현 시 별도 설정 없이 노출된다.
  - `kafka_consumergroup_lag` 등 (kafka-exporter, 브로커 측):
    `compose.observability.yaml`의 `kafka-exporter` 서비스(observability 프로필)가
    브로커의 커밋된 consumer group offset을 스크랩한다. 앱 계측과 무관하게 동작하지만
    **consumer group이 offset을 커밋한 적이 없으면 시리즈가 비어 있다** — 현재처럼
    컨슈머 미구현 상태에서는 경로(target UP)만 확보된 것이 완료 기준이다.
  - 결정(2026-10-18): alert 룰 `KafkaConsumerLagHigh`는 앱 메트릭
    (`kafka_consumer_fetch_manager_records_lag_max`) 기준을 **유지**한다
    (인스턴스·client_id 단위 탐지용). exporter 메트릭은 그룹·토픽 단위 개요
    대시보드용으로 사용한다. 양쪽 모두 컨슈머 구현 전까지는 발화/데이터 없음이 정상.

## 4. 보존·샘플링

- Prometheus 스크레이프 15s(기존 `prometheus.yml` 유지). 로컬 보존은 Prometheus 기본(15d).
- trace 샘플링은 `platform-defaults.yml`의 `TRACE_SAMPLING_PROBABILITY`(기본 0.1)를 따른다.
  로컬에서만 1.0으로 올려 디버깅할 수 있으나 기본값은 바꾸지 않는다.
- Loki/Tempo 보존은 각 설정 파일의 168h를 초기값으로 둔다. 금융 보존 정책은 미정 — 임의 확정 금지.
