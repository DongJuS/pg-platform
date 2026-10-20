# RUNBOOK — PG 플랫폼 알림 대응 절차 (초안, MVP2-F12)

> 각 alert의 `runbook_url` 어노테이션은 이 문서의 앵커를 가리킨다.
> 모든 임계값은 초기값/튜닝 대상이다. 실제 장애 대응 책임·에스컬레이션 체계는 미정.

## 목차

- [Watchdog](#watchdog)
- [ServiceDown](#servicedown)
- [HighHttp5xxErrorRate](#highhttp5xxerrorrate)
- [HighLatencyP95 / HighLatencyP99](#highlatencyp95p99)
- [KafkaConsumerLagHigh](#kafkaconsumerlaghigh)
- [OutboxBacklogHigh / OutboxOldestEventStale / OutboxMetricsUnavailable](#outboxbacklog)
- [ReconciliationMismatch](#reconciliationmismatch)

---

## Watchdog

- 의미: 항상 발화하는 룰. 이 알림이 **안 보이면** Prometheus→Alertmanager 경로가 끊긴 것.
- 절차:
  1. Alertmanager UI(`http://localhost:<포트>/#/alerts`)에 Watchdog이 있는지 확인.
  2. 없으면 Prometheus 타깃 페이지에서 alertmanager:9093 연결 상태 확인.
  3. `prometheus.yml`의 `alerting.alertmanagers`와 alertmanager 컨테이너 기동 상태 점검.

## ServiceDown

- 의미: 스크레이프 대상이 2분 이상 응답 없음.
- 절차:
  1. Grafana "PG Platform — 개요" 대시보드에서 어느 서비스가 빠졌는지 확인.
  2. `docker ps`로 해당 컨테이너 상태·재시작 여부 확인, `docker logs <서비스>`로 마지막 로그 확인.
  3. 컨테이너는 살아 있으나 `/actuator/prometheus`가 안 열리면 헬스(`:/actuator/health`) 확인.
  4. mock-pg 다운은 MVP2 승인·취소·환불을 막는다. 타임아웃을 승인·미처리로 단정하지 않고 UNKNOWN과 예약액을 보존한다. 나머지 mock은 해당 MVP 활성 여부를 확인한다.
  5. 재기동 후에도 반복되면 메모리 제한(`APP_MEMORY_LIMIT`, 기본 384m) 초과 여부 확인.

## HighHttp5xxErrorRate

- 의미: 5분 윈도우 5xx 비율이 초기값 2%를 10분간 초과.
- 절차:
  1. 개요 대시보드의 오류율 패널에서 서비스와 시작 시점 확인.
  2. 해당 서비스 로그(Loki 연결 전까지는 `docker logs`)에서 5xx 스택트레이스 확인.
  3. 의존 자원(DB/Redis/Kafka) 헬스와 `ServiceDown` 동시 발화 여부 확인.
  4. 특정 엔드포인트 집중이면 `uri` 라벨별 오류율을 Prometheus에서 조회:
     `sum by (uri) (rate(http_server_requests_seconds_count{status=~"5.."}[5m]))`.
  5. 스켈레톤 단계에서는 미구현 경로 호출이 5xx로 보일 수 있음 — 호출 경로가 의도된 것인지 먼저 확인.

## HighLatencyP95P99

- 의미: p95 1s / p99 2s(초기값)를 10분간 초과.
- 절차:
  1. 개요 대시보드 p95 패널에서 서비스 확인, 요청률 패널과 함께 부하 급증 여부 판단.
  2. Tempo(trace)에서 해당 시간대 느린 trace를 찾아 어느 구간(DB, Kafka, 대외 mock)인지 확인.
     - 단, 로컬 기본 샘플링 0.1이라 trace가 없을 수 있다.
  3. DB 구간이면 커넥션 풀/슬로우 쿼리, 대외 구간이면 mock 응답 지연 설정 확인.
  4. 임계값은 미튜닝 초기값이므로 반복 오탐 시 근거 데이터와 함께 조정을 제안한다.

## KafkaConsumerLagHigh

- 의미: 토픽·컨슈머별 최대 lag이 15분간 1000건(초기값) 초과.
- 전제(2026-10-18 확정, WO-3): 룰이 쓰는 `kafka_consumer_fetch_manager_records_lag_max`는
  Spring Boot 3.2+의 Micrometer 자동 등록으로 **`@KafkaListener` 컨슈머가 실제 생성되는
  순간 자동 노출**된다. MVP1 Transfer의 ledger.posted 소비자는 구현됐다.
  소비자가 활성화된 인스턴스의 실제 지표를 확인한다. Payment 등 나머지 소비자는 구현 전이다.
  보완 경로로 kafka-exporter(observability 프로필)가 브로커 측 `kafka_consumergroup_lag`를
  제공한다(consumer group이 offset을 커밋해야 시리즈가 생긴다). 룰은 앱 메트릭 기준을
  유지하고 exporter 메트릭은 대시보드용이다(METRICS.md §3 참조).
- 절차:
  1. 어느 토픽(`ledger.posted`, `transfer.completed`, `transfer.outbound`, `fep.ack`,
     `payment.approved`)과 컨슈머인지 확인.
  2. 해당 컨슈머 서비스 로그에서 처리 예외·재시도 루프 확인.
  3. `kafka` 컨테이너 헬스 확인. 브로커 문제면 lag과 함께 produce도 실패한다.
  4. 처리량 문제(정상 소비 중 단순 적체)면 초기값 재조정 후보로 기록한다.

## OutboxBacklog

- 의미: 미발행 outbox_events가 500건(초기값) 초과, 또는 최장 미발행 이벤트가 5분 초과.
- 전제: 두 지표와 `pg_outbox_metrics_sample_success`는 libs/outbox에서 구현됐다.
  최근 DB 표본 실패 시 건수/나이는 NaN이다. `OutboxMetricsUnavailable`은 수집 실패가
  2분간 계속될 때 발화한다. 수집 실패를 미발행 0건으로 해석하지 않는다.
- 절차:
  1. 서비스 DB에서 직접 확인: `SELECT COUNT(*) FROM outbox_events WHERE published_at IS NULL;`
  2. relay 로그에서 폴링 실패·lease 충돌·Kafka 전송 오류 확인.
  3. Kafka 브로커 헬스 확인(발행 실패가 누적의 일반 원인).
  4. 복구 후 누적분이 자연 소진되는지 확인. 강제 재발행은 이벤트 계약상 중복 위험이 있으므로
     libs/outbox의 relay 재처리 경로만 사용한다.

## ReconciliationMismatch

- 의미: 대사 불일치 1건 이상 감지(critical). Payment의 실제 Counter가 연결됐다.
  PG 누락·중복·금액/통화/상태, 내부 미확정, 실제 원장 계좌별 분개 금액을 조회한 run 결과를 조사한다. 자동 금액 보정·F17 종결 이력은 보류다.
- 절차:
  1. 알림의 service/topic 라벨과 발생 시각을 기록한다.
  2. payment 서비스 로그와 대사 대상 원장(ledger) 데이터를 시각 기준으로 대조한다.
  3. 불일치 건을 수동으로 정정하기 전에 반드시 원인(유실/중복/반올림/타임존)을 분류해 기록한다.
  4. 금액 관련 정정은 산식이 확정되기 전까지 자동화하지 않는다.
