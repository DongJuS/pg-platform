# Docker 실제 이체 1건 E2E 검증 — 2026-10-19

**판정: 현재 구현된 이체 경로 통과(25/25 검사).**
계좌 기동 우회 설정과 테스트용 초기 잔액 준비를 사용했다. OTP 검증은 미구현으로 이번 통과 범위에 포함하지 않는다.

## 실행 조건

- 실행 시각(KST): 2026-10-19T10:21:30.471986+09:00 ~ 2026-10-19T10:21:44.183913+09:00
- 격리 프로젝트: `pg_transfer_e2e_1a0faba2`. 기존 `sinhan_project_docker`의 컨테이너·DB·Kafka·포트는 재사용하지 않았다.
- 코드: HEAD `4dee7de1e2eaab630e8c61c240d9322460578d2c` 및 현재 미커밋 작업 트리. 현 코드로 Gateway/Auth/Account/Ledger/Transfer 이미지를 새로 빌드했다.
- 이미지 태그: `sinhan_project_e2e/<service>:20261019-437aed46` (ARM64).
- 설정: canonical `compose.yaml` + `compose.outbox.yaml` + 기존 `compose.e2e-account-proxy-workaround.yaml`을 해석한 별도 구성.
- 신규 DB·볼륨·Kafka·Redis를 사용하고 Gateway 포트는 자동 배정했다. 기존 스택의 컨테이너 식별자가 그대로인 것도 확인했다.

## 실제 요청과 결과

모든 회원가입·로그인·계좌 개설·이체·잔액/내역 조회는 Gateway HTTP API를 통해 수행했다.
검증용 사용자 두 명이 각자 자신의 계좌를 개설했다. 초기 자금 API가 없으므로 출금 계좌 100,000원과 입금 계좌 0원의 원장 행만 SQL fixture로 준비했다.
이후 이체 결과·분개·완료 이력·Outbox·Kafka 메시지를 수동 생성하거나 수정하지 않았다.

`POST /api/v1/transfers`는 **정확히 1회** 요청했다. 요청 금액은 **10,000 KRW**이며 서로 다른 사용자의 출금/입금 계좌를 사용했다.

| 계좌 | 이체 전 | 이체 후 | 변화 |
| --- | ---: | ---: | ---: |
| 출금 계좌 | 100,000원 | 90,000원 | −10,000원 |
| 입금 계좌 | 0원 | 10,000원 | +10,000원 |
| 합계 | 100,000원 | 100,000원 | 0원 |

- 이체 HTTP 응답: **200 / COMPLETED**.
- transferId: `750fc15c-1ff7-4173-8d96-96ee664420d2`
- transactionId: `ff8c93e3-958a-4f0f-9bc3-1e6d9186574b`
- journalId: `6577fa4c-c8d1-4ff6-9d3d-2e54ebd2d8f4`
- PostgreSQL: 완료된 이체 행 **1개**, 상태 전이 `RECEIVED → PROCESSING → COMPLETED`.
- MariaDB: journal **1개**, posting **2개**(−10,000/+10,000 KRW), 분개 합계 **0**.
- Gateway로 다시 조회한 두 계좌 잔액·거래내역과 이체 상태가 DB의 거래 식별자와 일치했다.

## 이벤트 발행과 소비

- Ledger Outbox: `ledger.posted` **1행**, `published_at` 설정 확인.
- Transfer Outbox: `transfer.completed` **1행**, `published_at` 설정 확인.
- 별도 Kafka 소비자로 두 토픽을 각각 읽어 eventId/transactionId/journalId를 DB 및 응답과 대조했다.
- 신규 브로커의 각 토픽 파티션 종료 오프셋 합계는 각각 **1**이었다.
- `transfer` 소비자 그룹은 `ledger.posted`를 처리해 커밋했고 해당 파티션 lag는 **0**이었다.
- REST 완료와 이벤트 처리가 모두 지난 뒤에도 `COMPLETED` 전이 이력은 **1건**이었다.

이는 이번 정상 경로의 관측 결과다. 장애·재전달을 포함한 exactly-once 보장을 뜻하지 않으며, 확정 ADR의 at-least-once 전제는 유지한다.

## 통과 범위와 남은 조건

- **기동 우회 사용:** AccountApplicationService의 `final`/트랜잭션 프록시 문제는 기존 임시 JDK 프록시 설정으로 우회했다. 기본 구성의 기동 오류를 수정한 결과가 아니다.
- **초기 잔액 fixture:** 계좌 개설과 초기 자금 공급까지 연결한 완전한 은행 업무 검증은 아니다. 입금/자금 준비 API는 이번 범위 밖이다.
- **OTP 미검증:** 요청에는 명시적인 테스트 placeholder를 넣었다. JWT 발급·검증은 실제 경로를 통과했지만 이체 step-up 인증은 미구현이다.
- 로그아웃 토큰 철회, 타인 계좌 권한, 잔액 부족 오류 응답 등 앞선 검토의 별도 결함을 수정한 검증은 아니다.
- 금융 처리 코드는 수정하지 않았다. 이번 변경은 이 검증 보고서와 비밀값을 제외한 증거 파일만 추가한다.

근거: Figma 시스템 아키텍처 구성도 `19:2`, 동기 REST 표기 `24:42`, accepted ADR 0001(Transfer → Ledger 동기 REST, 각 서비스의 로컬 트랜잭션·Outbox, Ledger/Transfer의 이벤트 책임).

[상세 검사 결과 및 이벤트 원문](evidence/transfer-e2e-2026-10-19.json)

## 검증 후 정리

검증 전용 컨테이너·테스트 볼륨과 임시 비밀 설정 파일은 삭제했다. 기존 실행 스택의 컨테이너 식별자가 그대로 유지되는 것을 재확인했다. 위 API 주소는 검증 시점의 임시 주소다.
