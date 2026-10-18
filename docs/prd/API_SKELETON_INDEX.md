# API 스켈레톤 파일 목록

기능명세·Notion REST API DB·Figma와 최신 사용자 결정을 대조한 파일 목록이다. 아래 경로·DTO·정상 상태는 최종 계약 전 초안이다. 보류 API를 구현 완료로 표시하지 않는다.

| 기능 ID | 서비스 | 메서드·경로 | 현재 상태 | Controller |
| --- | --- | --- | --- | --- |
| MVP1-F01 | auth | POST /api/v1/auth/register | 초안·개발용 501 | [AuthController](../../main-vpc/core/auth/src/main/java/com/shinhan/pg/auth/adapter/in/web/AuthController.java) |
| MVP1-F02 | auth | POST /api/v1/auth/login | 초안·개발용 501 | [AuthController](../../main-vpc/core/auth/src/main/java/com/shinhan/pg/auth/adapter/in/web/AuthController.java) |
| MVP1-F03 | auth | POST /api/v1/auth/refresh | 초안·개발용 501 | [AuthController](../../main-vpc/core/auth/src/main/java/com/shinhan/pg/auth/adapter/in/web/AuthController.java) |
| MVP1-F04 | auth | POST /api/v1/auth/logout | 초안·개발용 501 | [AuthController](../../main-vpc/core/auth/src/main/java/com/shinhan/pg/auth/adapter/in/web/AuthController.java) |
| MVP1-F05 | auth | GET /api/v1/users/me | 초안·개발용 501 | [UserProfileController](../../main-vpc/core/auth/src/main/java/com/shinhan/pg/auth/adapter/in/web/UserProfileController.java) |
| MVP1-F06 | auth | POST /api/v1/auth/otp/verify | 초안·개발용 501 | [AuthController](../../main-vpc/core/auth/src/main/java/com/shinhan/pg/auth/adapter/in/web/AuthController.java) |
| MVP1-F07 | account | POST /api/v1/accounts | 초안·개발용 501 | [AccountController](../../main-vpc/core/account/src/main/java/com/shinhan/pg/account/adapter/in/web/AccountController.java) |
| MVP1-F08 | account | GET /api/v1/accounts | 초안·개발용 501 | [AccountController](../../main-vpc/core/account/src/main/java/com/shinhan/pg/account/adapter/in/web/AccountController.java) |
| MVP1-F09 | account | GET /api/v1/accounts/{accountId} | 초안·개발용 501 | [AccountController](../../main-vpc/core/account/src/main/java/com/shinhan/pg/account/adapter/in/web/AccountController.java) |
| MVP1-F10 | account | PATCH /api/v1/accounts/{accountId}/status | 초안·개발용 501 | [AccountController](../../main-vpc/core/account/src/main/java/com/shinhan/pg/account/adapter/in/web/AccountController.java) |
| MVP1-F11 | ledger | GET /api/v1/accounts/{accountId}/balances | 초안·개발용 501 | [LedgerAccountController](../../main-vpc/core/ledger/src/main/java/com/shinhan/pg/ledger/adapter/in/web/LedgerAccountController.java) |
| MVP1-F12 | ledger | GET /api/v1/accounts/{accountId}/entries | 초안·개발용 501 | [LedgerAccountController](../../main-vpc/core/ledger/src/main/java/com/shinhan/pg/ledger/adapter/in/web/LedgerAccountController.java) |
| MVP1-F13 | ledger | POST /internal/v1/ledger/postings | 초안·개발용 501 | [InternalLedgerController](../../main-vpc/core/ledger/src/main/java/com/shinhan/pg/ledger/adapter/in/web/InternalLedgerController.java) |
| MVP1-F14 | ledger | GET /internal/v1/ledger/transactions/{transactionId} | 초안·개발용 501 | [InternalLedgerController](../../main-vpc/core/ledger/src/main/java/com/shinhan/pg/ledger/adapter/in/web/InternalLedgerController.java) |
| MVP1-F15 | transfer | POST /api/v1/transfers | 초안·개발용 501 | [TransferController](../../main-vpc/core/transfer/src/main/java/com/shinhan/pg/transfer/adapter/in/web/TransferController.java) |
| MVP1-F16 | transfer | GET /api/v1/transfers/{transferId} | 초안·개발용 501 | [TransferController](../../main-vpc/core/transfer/src/main/java/com/shinhan/pg/transfer/adapter/in/web/TransferController.java) |
| MVP1-F17 | transfer | GET /api/v1/transfers | 초안·개발용 501 | [TransferController](../../main-vpc/core/transfer/src/main/java/com/shinhan/pg/transfer/adapter/in/web/TransferController.java) |
| MVP2-F01 | payment | POST /api/v1/payments | 초안·개발용 501 | [PaymentController](../../main-vpc/core/payment/src/main/java/com/shinhan/pg/payment/adapter/in/web/PaymentController.java) |
| MVP2-F02 | payment | GET /api/v1/payments/{paymentId} | 초안·개발용 501 | [PaymentController](../../main-vpc/core/payment/src/main/java/com/shinhan/pg/payment/adapter/in/web/PaymentController.java) |
| MVP2-F03 | payment | POST /api/v1/payments/{paymentId}/cancel | 초안·개발용 501 | [PaymentController](../../main-vpc/core/payment/src/main/java/com/shinhan/pg/payment/adapter/in/web/PaymentController.java) |
| MVP2-F04 | payment | POST /api/v1/payments/{paymentId}/refunds | 초안·개발용 501 | [PaymentController](../../main-vpc/core/payment/src/main/java/com/shinhan/pg/payment/adapter/in/web/PaymentController.java) |
| MVP2-F05 | payment | GET /api/v1/payments/{paymentId}/refunds | 초안·개발용 501 | [PaymentController](../../main-vpc/core/payment/src/main/java/com/shinhan/pg/payment/adapter/in/web/PaymentController.java) |
| MVP2-F06 | payment | POST /mock/v1/pg/webhooks | 초안·개발용 501 | [PaymentWebhookController](../../main-vpc/core/payment/src/main/java/com/shinhan/pg/payment/adapter/in/web/PaymentWebhookController.java) |
| MVP2-F08 | payment | POST /internal/v1/payments/reconciliation-runs | 초안·개발용 501 | [PaymentReconciliationController](../../main-vpc/core/payment/src/main/java/com/shinhan/pg/payment/adapter/in/web/PaymentReconciliationController.java) |
| MVP2-F09 | payment | GET /internal/v1/payments/reconciliation-runs/{runId} | 초안·개발용 501 | [PaymentReconciliationController](../../main-vpc/core/payment/src/main/java/com/shinhan/pg/payment/adapter/in/web/PaymentReconciliationController.java) |
| MVP3-F01 | ai-gateway | POST /internal/v1/ai/inference | 보류·매핑 없음 | [AiInferenceController](../../ai-vpc/ai-gateway/src/main/java/com/shinhan/pg/aigateway/adapter/in/web/AiInferenceController.java) |
| MVP3-F02 | ai-gateway | GET /internal/v1/ai/requests/{requestId} | 보류·매핑 없음 | [AiAuditController](../../ai-vpc/ai-gateway/src/main/java/com/shinhan/pg/aigateway/adapter/in/web/AiAuditController.java) |
| MVP4-F01 | wealth-report | POST /api/v1/wealth/reports | 초안·개발용 501 | [WealthReportController](../../main-vpc/info/wealth-report/src/main/java/com/shinhan/pg/wealthreport/adapter/in/web/WealthReportController.java) |
| MVP4-F02 | wealth-report | GET /api/v1/wealth/reports/{reportId} | 초안·개발용 501 | [WealthReportController](../../main-vpc/info/wealth-report/src/main/java/com/shinhan/pg/wealthreport/adapter/in/web/WealthReportController.java) |
| MVP4-F03 | wealth-report | GET /api/v1/wealth/reports | 초안·개발용 501 | [WealthReportController](../../main-vpc/info/wealth-report/src/main/java/com/shinhan/pg/wealthreport/adapter/in/web/WealthReportController.java) |
| MVP4-F04 | wealth-report | POST /internal/v1/wealth/reports/{reportId}/retry | 보류·매핑 없음 | [ReportOperationsController](../../main-vpc/info/wealth-report/src/main/java/com/shinhan/pg/wealthreport/adapter/in/web/ReportOperationsController.java) |
| MVP5-F01 | transfer | POST /api/v1/transfers/overseas | 초안·개발용 501 | [OverseasTransferController](../../main-vpc/core/transfer/src/main/java/com/shinhan/pg/transfer/adapter/in/web/OverseasTransferController.java) |
| MVP5-F02 | transfer | GET /api/v1/transfers/overseas/{transferId} | 초안·개발용 501 | [OverseasTransferController](../../main-vpc/core/transfer/src/main/java/com/shinhan/pg/transfer/adapter/in/web/OverseasTransferController.java) |
| MVP5-F04 | fep-adapter | POST /internal/v1/fep/swift/messages | 초안·개발용 501 | [FepSwiftController](../../fep-vpc/fep-adapter/src/main/java/com/shinhan/pg/fepadapter/adapter/in/web/FepSwiftController.java) |
| MVP6-F02 | aml | GET /internal/v1/aml/cases | 초안·개발용 501 | [AmlCaseController](../../main-vpc/info/aml/src/main/java/com/shinhan/pg/aml/adapter/in/web/AmlCaseController.java) |
| MVP6-F03 | aml | POST /internal/v1/aml/cases/{caseId}/reports | 초안·개발용 501 | [AmlReportController](../../main-vpc/info/aml/src/main/java/com/shinhan/pg/aml/adapter/in/web/AmlReportController.java) |
| MVP7-F03 | fep-adapter | POST /internal/v1/fep/settlement/files | 초안·개발용 501 | [FepSettlementController](../../fep-vpc/fep-adapter/src/main/java/com/shinhan/pg/fepadapter/adapter/in/web/FepSettlementController.java) |
| MVP7-F04 | settlement | GET /internal/v1/settlements/{date} | 초안·개발용 501 | [SettlementController](../../main-vpc/core/settlement/src/main/java/com/shinhan/pg/settlement/adapter/in/web/SettlementController.java) |
| MVP7-F04 | settlement | POST /internal/v1/settlements/{date}/rerun | 초안·개발용 501 | [SettlementController](../../main-vpc/core/settlement/src/main/java/com/shinhan/pg/settlement/adapter/in/web/SettlementController.java) |
| MVP3-F04 | ai-gateway | GET /internal/v1/ai/security-statistics | 보류·매핑 없음 | [AiStatisticsController](../../ai-vpc/ai-gateway/src/main/java/com/shinhan/pg/aigateway/adapter/in/web/AiStatisticsController.java) |
| MVP5-F07 | swift | GET /internal/v1/swift/transfers/{uetr}/messages | 보류·매핑 없음 | [SwiftOperationsController](../../main-vpc/external/swift/src/main/java/com/shinhan/pg/swift/adapter/in/web/SwiftOperationsController.java) |

## HTTP가 아닌 진입점

- SWIFT: transfer.outbound 소비, 같은 UETR 재전송, pacs.002 판정 후 fep.ack 의도 기록.
- Ledger: 내부 분개/조회 외 fep.ack 기반 확정·역분개 처리.
- Transfer: ledger.posted 완료 반영과 UNKNOWN 결과 재조회.
- Payment: 검증된 PG 결과 반영과 UNKNOWN 복구.
- Wealth Report: 거래 완료 스냅샷 갱신과 중단된 동기 생성 작업 복구.
- AML: 거래 이벤트 룰 탐지·자동 보고 판정·보고 전송 재시도.
- Settlement: 마감·고정 입력 집계·파일 생성/송수신·대사·원장 반영·재개.

KafkaListener·배치 스케줄은 등록하지 않았다. 해당 파일의 TODO에서 topic·payload·ack/재시도·중복 효과·cron/범위를 확정하도록 안내한다. FEP PG/KoFIU/결과 callback, Gateway 라우팅, Mock 기관 경로는 별도 이름의 파일에 후보 주석을 뒀다. Actuator health/prometheus는 기존 설정에서 제공하며 중복 Controller를 만들지 않는다.

## 미정과 충돌 처리

- Notion의 과거 7개 서비스·PostgreSQL 단독·보고서 비동기 접수 표기는 최신 Figma/사용자 결정으로 대체한다.
- AI 차단 원문 저장은 확정하지 않는다. 중계 기본 감사는 원문 제외이며 감사 조회/쿼터/통계는 보류다.
- 401/403/409/422/429 및 외부 장애 응답은 업무 구현할 계약 후보다. 현재 테스트가 해당 금융 판단을 검증했다는 의미가 아니다.
- 금액 단위·필수성·페이지네이션·서비스 인증·callback 전달·서명 헤더는 구현 전 최종 대조한다. Controller의 신원 attribute는 내부 연결 자리다.
- 원장 동일 transactionId의 결과 재조회와 사용자 중복 이체의 409는 다르다. 이체 복구 조회 404만으로 최종 실패를 선언하지 않는다.
