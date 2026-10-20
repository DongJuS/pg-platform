# Payment MVP2 implementation checklist

이 파일명은 기존 진입점을 유지한다. 현재 활성 `skeleton` 프로필의 MVP2 경로는 실제 PostgreSQL/JDBC·REST·원장 연동이다. 기본/aws 활성화와 고객 스택 배포는 별도이며 이번 작업에서 적용하지 않았다.

## 확정한 금융 동작

- [x] 승인·부분 환불·전체 취소 모두 PG 최종 결과와 원장 반영을 확인한 뒤에만 내부 성공을 응답한다.
- [x] 승인은 고객의 `sourceAccountId`에서 요청한 `destinationAccountId`로 분개한다. 환불·취소는 원거래 계좌 역방향이다. 요청액과 내부 확정 승인액을 구분한다.
- [x] 금융 변경 요청은 업무 ID당 한 번만 전송한다. 접수 ID와 최종 결과는 다르며, 타임아웃·연결 실패·서킷 오픈을 승인으로 취급하지 않는다.
- [x] 결과 불명은 UNKNOWN 및 예약액으로 남긴다. 중복 요청이 신규 승인·환불·취소를 만들지 않는다.
- [x] 원문 HMAC-SHA256 검증 후 FEP가 원문·서명을 내부 서비스 인증과 함께 Payment로 전달한다. Payment도 서명을 검증하고 동기 응답과 같은 반영 경로를 사용한다.
- [x] eventId 재전달과 operationId의 금융 효과 중복을 각각 방어한다. 이전 버전 이벤트가 완료 상태를 되돌리지 않는다.
- [x] 확정 환불액 + 처리 중 예약액 + 신규 요청액 <= 승인액. Payment 행 잠금과 DB CHECK 제약으로 보호한다.
- [x] 처리 중·불명 환불과 취소는 충돌 거절한다. 부분 환불 후 전체 취소를 허용하지 않는다.
- [ ] 자동 결과 조회 허용 여부는 사용자 확인 대기다. `pg.payment.recovery-queries-enabled=false`가 기본이며 스케줄은 없다. 조회 복구는 opt-in 구현·테스트만 했고 실행 스택에서는 켜지 않았다. 조회가 404이면 금융 요청을 다시 보내지 않는다.

사용자 확정 결정: 로컬 ADR `docs/adr/0002-mvp2-synchronous-payments.md`. 이 저장소는 `docs/adr/`를 원래부터 ignore하므로 Git에 남는 결정 요약은 이 체크리스트와 아래 evidence에도 함께 저장한다.

## 기능·활성 경로

| 기능 | 활성 경로 / 구현 | 검증 범위 |
| --- | --- | --- |
| F01 승인 | POST `/api/v1/payments` | Gateway → Payment → FEP → Mock PG, PG 최종 결과 → Ledger 분개 → Payment 완료/Outbox |
| F02 조회 | GET `/api/v1/payments/{paymentId}` | 소유권, requestedAmount/approvedAmount/refundedAmount/reservedAmount/status |
| F03 취소 | POST `/api/v1/payments/{paymentId}/cancel` | PG 취소와 역분개 확인, 중복 취소/환불 충돌 방어 |
| F04·F05 환불 | POST/GET `/api/v1/payments/{paymentId}/refunds` | 실제 DB 동시성, 예약·확정액 한도, 소유권·멱등성·이력 |
| F06 웹훅 | Mock PG → FEP POST `/callbacks/v1/pg` → Payment POST `/internal/v1/payments/pg/webhooks` | 양쪽 원문 서명, 내부 인증·FEP 호출자, 중복·역순, 원장 반영 후 ACK |
| F07 CB | Payment의 FEP client | 초기값 5회 실패·30초 차단·단일 half-open probe. 임계값은 SLO가 아니며 설정 가능 |
| F08·F09 대사 | POST `/internal/v1/payments/reconciliation-runs`, GET `.../{runId}` | Payment·PG 파일·Ledger의 실제 계좌별 분개 금액 비교, 누락·중복·금액·상태 불일치, 결과/Outbox 저장 |
| F10·F11 | 기존 Actuator·Prometheus | Payment·FEP·Mock PG 실제 DB health와 지표 노출·수집 |
| F12 관측성 | 기존 Collector·Prometheus·Grafana·Loki·Tempo·Alertmanager 재사용 | 실제 결제 추적·로그 연계, 대사 Counter·PG 지연 Histogram·Outbox 적체/수집실패 지표, 로컬 알림 발화 |
| F14 복구 | POST `/internal/v1/payments/operations/{operationId}/recover` | 기본 비활성. opt-in 테스트에서 동일 ID PG/원장 결과 조회만 사용, 재전송 없음 |
| F15·F16 | PaymentStore + 순수 정책 | 짧은 로컬 잠금 트랜잭션, 효과별 전송 표식·PG 버전·원장 ID·완료·Outbox 보호 |
| F13·F17 | 보류 유지 | 상태 통합 API·불일치 처리 이력·자동 금액 보정 미구현 |

F08은 비교를 마친 뒤 200/COMPLETED를 반환한다. 각 서비스에서 읽은 결과를 비교하며 분산 DB 스냅샷을 가정하지 않는다. 진행 중 거래가 있는 당일 대사는 일시적 차이가 있을 수 있으며 자동 보정하지 않는다. 프로세스가 중단된 RUNNING 대사의 운영 종결은 별도 운영 절차가 필요하다.

## 실제 저장과 서비스 경계

- Payment V3: payments / payment_operations / pg_events, 한도 CHECK·멱등 unique·소유권·행 잠금. V4: 대사 run / difference. 적용된 기존 V1/V2는 변경하지 않는다.
- FEP V2: 접수·최종 결과·한 번의 dispatch 표식·서명 callback 원문. Mock PG V1: 독립 PostgreSQL의 합성 승인/환불 상태·재시작 후 동일 결과·실패 시나리오.
- Ledger는 기존 MariaDB 쓰기 경로를 재사용한다. 대사는 인증된 내부 `GET /internal/v1/ledger/transactions/{id}/postings`로 읽는다. 다른 서비스 DB JOIN은 없다.
- Payment 완료와 `payment.approved` / `payment.refunded` / `payment.cancelled`, 대사 불일치와 `recon.mismatch` Outbox는 각각 Payment의 같은 로컬 트랜잭션이다. Ledger는 계속 `ledger.posted`, Transfer는 `transfer.completed`를 소유한다.
- 빈 자격증명은 닫힌 상태로 실패한다. JSON 원문·서명 키·인증 토큰·계좌/금액을 새 업무 로그에 남기지 않는다. 새 client는 redirect를 따르지 않고 기존 ObservationRegistry를 사용한다.
- 기존 미사용 skeleton port/mapper 및 다른 MVP의 TODO는 활성 업무 경로가 아니다. 파일 존재만으로 기능 완료를 판단하지 않는다.

## Figma 대조

이번 작업에서 최신 `1:9`(mvp2_결제서비스_SRE)를 재조회했다. `9:143`/`9:186`은 승인·환불 분개, `9:171`은 eventId/version, `9:210`은 환불 한도, `9:220`/`9:223`은 알림·Runbook 근거다. 기존 코드에는 실제 본문과 DB 제약이 없었으며 위 활성 경로로 구현했다. 도식에 없는 자금 계좌·완료 시점·재전송 제한은 사용자 결정을 적용했다. 도식의 EKS/PG account_db 등은 사용자 확정 ECS Fargate·MariaDB보다 우선하지 않는다. Mock PG의 독립 DB는 Mock 재시작 정합성을 위한 구현 선택이며 Figma가 DB 배치를 확정했다고 주장하지 않는다.

## 실행·증거

```sh
# 새 프로젝트·동적 localhost 포트·새 DB/볼륨에서 검증하고 테스트 자원만 정리
python3 infra/docker/run-mvp1-e2e.py --suite MVP2 --observability --output /private/tmp/mvp2-result.json
./gradlew build -Pintegration=true
```

검증 최종 결과·소스 해시·보존 여부는 [진행/검증 기록](../../../infra/docker/evidence/mvp2-progress-2026-10-20.json), [분리 Docker 증거](../../../infra/docker/evidence/mvp2-business-2026-10-20.json)를 따른다. 이전 269/69 기록은 기반 작업 당시 증거이며 최종 MVP2 결과와 구분한다. AWS·실기관·고객 스택 배포는 수행하지 않는다.
