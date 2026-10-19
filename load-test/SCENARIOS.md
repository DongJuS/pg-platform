# 부하시험 시나리오 카탈로그 (MVP7-F05)

대상: 로컬 Docker 스택(`infra/docker/compose.yaml` + 필요 시 `compose.observability.yaml`, 프로젝트 `sinhan_project_docker`, AWS 대체 환경). 구 `infra/docker-compose` 스택은 비canonical.
확정 결정 전제: **Outbox DB 폴링**, **원장 비관적 잠금(동일 계좌 동시 출금)**, **보고서 동기 응답**.

> 상태 표기: `[지금 실행 가능]` = 스켈레톤(501) 상태에서도 의미 있는 측정 가능.
> `[업무 구현 후]` = 501 스켈레톤 상태에서는 실행해도 업무 검증이 불가능하며,
> 스크립트는 501을 `not_implemented`로만 집계한다(가짜 성공 금지).

---

## 1. 단일 계좌 경합 — 동일 계좌 동시 출금 `[업무 구현 후]`

- **목적**: 원장 비관적 잠금이 동일 출금 계좌의 동시 이체를 직렬화하고, 잔액이 음수가 되지 않는지 검증.
- **주입 방법**: `VARIANT=contention CONTENTION_ACCOUNT_ID=<계좌> RPS=100 ./run.sh transfer-create.js`. 모든 VU가 같은 출금 계좌로 `POST /api/v1/transfers`.
- **관찰 지표**:
  - 잔액 불변식: 시험 전후 `GET /api/v1/accounts/{accountId}/balances`(MVP1-F11) 스냅샷 비교 — `종료 잔액 = 시작 잔액 - Σ성공 출금액`, 잔액 ≥ 0. 분개 합계는 `GET /api/v1/accounts/{accountId}/entries`(MVP1-F12)로 대조.
  - 409(잔액 부족) 비율: `transfer_conflict` 카운터.
  - Kafka lag: `ledger.posted` 등 소비 지연 (측정 방법은 §5 참조).
- **판정 기준**: 잔액 불변식 위반 0건, 잠금 교착·타임아웃으로 인한 5xx 0건, p99 < `BIZ_P99_MS`.

## 2. 중복 요청 — 동일 멱등키 재전송 `[업무 구현 후]`

- **목적**: 같은 `Idempotency-Key`의 이체/결제가 N회 도달해도 1회만 반영되는지 검증.
- **주입 방법**: k6 스크립트 수정 없이 `RUN_ID`를 고정한 채 `transfer-create.js`를 2회 실행하면 동일 멱등키가 재사용된다(`RUN_ID=idem-test-1`로 1차→2차 연속 실행). 또는 클라이언트 재시도를 흉내 내려면 §4(응답 유실)와 결합.
- **관찰 지표**: 동일 키 2차 요청의 응답(최종 계약: 동일 결과 2xx 재반환 또는 409 — **확인 필요**), 원장 분개 건수 증가 없음(잔액 불변식).
- **판정 기준**: 멱등키 1개당 원장 반영 정확히 1회, 2차 응답이 1차와 모순되지 않음.

## 3. 동시 환불 `[업무 구현 후]`

- **목적**: 하나의 승인 건에 대한 동시 환불 요청이 환불 합계 ≤ 승인 금액을 보장하는지 검증.
- **주입 방법**: `./run.sh payment-approve-refund.js` — setup에서 승인 건 생성 후 전 VU가 동일 paymentId에 동시 환불.
- **관찰 지표**: `refund_ok` / `refund_conflict` 비율, 사후 `GET /api/v1/payments/{paymentId}/refunds`(MVP2-F05)로 환불 합계 대조.
- **판정 기준**: 환불 합계 > 승인 금액인 건 0, 5xx 0건.

## 4. 응답 유실 — 클라이언트가 결과를 못 받은 채 재시도 `[주입 도구 확인 필요]`

- **목적**: 서버 처리는 완료됐지만 응답이 유실된 상황에서, 동일 멱등키 재시도가 이중 반영 없이 수렴하는지 검증. Outbox DB 폴링 구조에서 이벤트는 1회만 발행돼야 한다.
- **주입 방법**: k6만으로는 불가. **Toxiproxy 컨테이너**(Shopify/toxiproxy)를 compose 네트워크에 추가하고 서비스↔DB 또는 서비스↔Kafka 구간에 `timeout`/`reset_peer` toxic을 걸어 응답을 끊는다. k6는 프록시 경유 엔드포인트로 동일 멱등키를 재전송. Toxiproxy 추가 여부·compose 반영은 **확인 필요**(기존 파일 수정 금지 규칙상 별도 override 파일 제안).
- **관찰 지표**: 멱등키당 원장 반영 1회, Outbox 이벤트 중복 발행 여부(Kafka 토픽 메시지 수 대조), 재시도 후 최종 상태 수렴.
- **판정 기준**: 이중 출금/이중 결제 0건, 재시도가 최종 상태를 조회 가능하게 복구.

## 5. 지연 이벤트 — Kafka 소비 지연과 복구 `[주입 도구 확인 필요]`

- **목적**: 컨슈머 지연 중 쌓인 lag이 복구 후 얼마나 빨리 해소되는지(복구 시간) 측정.
- **주입 방법**: (a) 대상 서비스 컨테이너 일시 중지(`docker pause`)로 소비 중단 후 재개, 또는 (b) Toxiproxy로 kafka:19092 구간에 `latency` toxic 주입 — **확인 필요**. k6는 그동안 업무 트래픽을 계속 주입.
- **관찰 지표**: Kafka lag — consumer group별 lag 시계열. 측정 경로: ① observability 프로필의 kafka-exporter (`kafka_consumergroup_lag`, prometheus job `kafka-exporter` — consumer가 offset을 commit해야 시리즈가 생긴다), ② kafka 컨테이너 내 `kafka-consumer-groups.sh --bootstrap-server localhost:19092 --describe --group <group>` 폴링. 복구 시간 = 재개 시점부터 lag이 임계값(예: < 100) 아래로 내려오기까지.
- **판정 기준**: lag 해소 시간이 목표(예: 60s — **확인 필요**) 이내, 지연 구간 중 요청 손실 없음(최종 일치).

## 6. 마감 재실행 `[업무 구현 후]`

- **목적**: 정산 마감 재실행(`POST /internal/v1/settlements/{date}/rerun`)이 멱등하고 집계를 이중 반영하지 않는지 검증.
- **주입 방법**: `./run.sh settlement-close.js` — 같은 날짜에 rerun 연속 2회 호출을 반복.
- **관찰 지표**: rerun 전후 `GET /internal/v1/settlements/{date}` 스냅샷 비교(집계 금액 불변), `settlement_rerun_conflict`(멱등 거절) 비율, 대사 결과(`reconciliation-runs`, MVP2-F08/F09)와의 일치.
- **판정 기준**: 재실행 후 집계 변화 0(또는 명시적 버전 증가만), 5xx 0건.

---

## 공통 관찰 지표 수집 방법

| 지표 | 수집 경로 |
| --- | --- |
| RPS·p95/p99·오류율 | k6 summary (results/*-summary.json) |
| 잔액 불변식 | 시험 전후 balances/entries API 스냅샷 비교(수동 또는 별도 스크립트 — **확인 필요**) |
| Kafka lag | §5 참조 (observability 프로필의 kafka-exporter 사용 가능; consumer skeleton 상태에서는 시리즈 없음) |
| 복구 시간 | 장애 주입→지표 정상화까지 wall-clock 측정 |
| 인프라 조건 | `docker stats`, compose mem_limit(앱 384m 등), 호스트 사양을 결과 템플릿에 기록 |
| 서비스 메트릭 | Prometheus(127.0.0.1:39090, dev-access 경유) `/actuator/prometheus` 스크랩 |
