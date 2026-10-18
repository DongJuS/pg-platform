# API Skeleton File List

This file list was cross-checked against the feature specification, the Notion REST API DB, Figma and the user's latest decisions. The paths, DTOs and success statuses below are drafts before the final contract. Deferred APIs are not marked as implemented.

| Feature ID | Service | Method and path | Current status | Controller |
| --- | --- | --- | --- | --- |
| MVP1-F01 | auth | POST /api/v1/auth/register | Draft, dev-only 501 | [AuthController](../../main-vpc/core/auth/src/main/java/com/shinhan/pg/auth/adapter/in/web/AuthController.java) |
| MVP1-F02 | auth | POST /api/v1/auth/login | Draft, dev-only 501 | [AuthController](../../main-vpc/core/auth/src/main/java/com/shinhan/pg/auth/adapter/in/web/AuthController.java) |
| MVP1-F03 | auth | POST /api/v1/auth/refresh | Draft, dev-only 501 | [AuthController](../../main-vpc/core/auth/src/main/java/com/shinhan/pg/auth/adapter/in/web/AuthController.java) |
| MVP1-F04 | auth | POST /api/v1/auth/logout | Draft, dev-only 501 | [AuthController](../../main-vpc/core/auth/src/main/java/com/shinhan/pg/auth/adapter/in/web/AuthController.java) |
| MVP1-F05 | auth | GET /api/v1/users/me | Draft, dev-only 501 | [UserProfileController](../../main-vpc/core/auth/src/main/java/com/shinhan/pg/auth/adapter/in/web/UserProfileController.java) |
| MVP1-F06 | auth | POST /api/v1/auth/otp/verify | Draft, dev-only 501 | [AuthController](../../main-vpc/core/auth/src/main/java/com/shinhan/pg/auth/adapter/in/web/AuthController.java) |
| MVP1-F07 | account | POST /api/v1/accounts | Draft, dev-only 501 | [AccountController](../../main-vpc/core/account/src/main/java/com/shinhan/pg/account/adapter/in/web/AccountController.java) |
| MVP1-F08 | account | GET /api/v1/accounts | Draft, dev-only 501 | [AccountController](../../main-vpc/core/account/src/main/java/com/shinhan/pg/account/adapter/in/web/AccountController.java) |
| MVP1-F09 | account | GET /api/v1/accounts/{accountId} | Draft, dev-only 501 | [AccountController](../../main-vpc/core/account/src/main/java/com/shinhan/pg/account/adapter/in/web/AccountController.java) |
| MVP1-F10 | account | PATCH /api/v1/accounts/{accountId}/status | Draft, dev-only 501 | [AccountController](../../main-vpc/core/account/src/main/java/com/shinhan/pg/account/adapter/in/web/AccountController.java) |
| MVP1-F11 | ledger | GET /api/v1/accounts/{accountId}/balances | Draft, dev-only 501 | [LedgerAccountController](../../main-vpc/core/ledger/src/main/java/com/shinhan/pg/ledger/adapter/in/web/LedgerAccountController.java) |
| MVP1-F12 | ledger | GET /api/v1/accounts/{accountId}/entries | Draft, dev-only 501 | [LedgerAccountController](../../main-vpc/core/ledger/src/main/java/com/shinhan/pg/ledger/adapter/in/web/LedgerAccountController.java) |
| MVP1-F13 | ledger | POST /internal/v1/ledger/postings | Draft, dev-only 501 | [InternalLedgerController](../../main-vpc/core/ledger/src/main/java/com/shinhan/pg/ledger/adapter/in/web/InternalLedgerController.java) |
| MVP1-F14 | ledger | GET /internal/v1/ledger/transactions/{transactionId} | Draft, dev-only 501 | [InternalLedgerController](../../main-vpc/core/ledger/src/main/java/com/shinhan/pg/ledger/adapter/in/web/InternalLedgerController.java) |
| MVP1-F15 | transfer | POST /api/v1/transfers | Draft, dev-only 501 | [TransferController](../../main-vpc/core/transfer/src/main/java/com/shinhan/pg/transfer/adapter/in/web/TransferController.java) |
| MVP1-F16 | transfer | GET /api/v1/transfers/{transferId} | Draft, dev-only 501 | [TransferController](../../main-vpc/core/transfer/src/main/java/com/shinhan/pg/transfer/adapter/in/web/TransferController.java) |
| MVP1-F17 | transfer | GET /api/v1/transfers | Draft, dev-only 501 | [TransferController](../../main-vpc/core/transfer/src/main/java/com/shinhan/pg/transfer/adapter/in/web/TransferController.java) |
| MVP2-F01 | payment | POST /api/v1/payments | Draft, dev-only 501 | [PaymentController](../../main-vpc/core/payment/src/main/java/com/shinhan/pg/payment/adapter/in/web/PaymentController.java) |
| MVP2-F02 | payment | GET /api/v1/payments/{paymentId} | Draft, dev-only 501 | [PaymentController](../../main-vpc/core/payment/src/main/java/com/shinhan/pg/payment/adapter/in/web/PaymentController.java) |
| MVP2-F03 | payment | POST /api/v1/payments/{paymentId}/cancel | Draft, dev-only 501 | [PaymentController](../../main-vpc/core/payment/src/main/java/com/shinhan/pg/payment/adapter/in/web/PaymentController.java) |
| MVP2-F04 | payment | POST /api/v1/payments/{paymentId}/refunds | Draft, dev-only 501 | [PaymentController](../../main-vpc/core/payment/src/main/java/com/shinhan/pg/payment/adapter/in/web/PaymentController.java) |
| MVP2-F05 | payment | GET /api/v1/payments/{paymentId}/refunds | Draft, dev-only 501 | [PaymentController](../../main-vpc/core/payment/src/main/java/com/shinhan/pg/payment/adapter/in/web/PaymentController.java) |
| MVP2-F06 | payment | POST /mock/v1/pg/webhooks | Draft, dev-only 501 | [PaymentWebhookController](../../main-vpc/core/payment/src/main/java/com/shinhan/pg/payment/adapter/in/web/PaymentWebhookController.java) |
| MVP2-F08 | payment | POST /internal/v1/payments/reconciliation-runs | Draft, dev-only 501 | [PaymentReconciliationController](../../main-vpc/core/payment/src/main/java/com/shinhan/pg/payment/adapter/in/web/PaymentReconciliationController.java) |
| MVP2-F09 | payment | GET /internal/v1/payments/reconciliation-runs/{runId} | Draft, dev-only 501 | [PaymentReconciliationController](../../main-vpc/core/payment/src/main/java/com/shinhan/pg/payment/adapter/in/web/PaymentReconciliationController.java) |
| MVP3-F01 | ai-gateway | POST /internal/v1/ai/inference | Deferred, no mapping | [AiInferenceController](../../ai-vpc/ai-gateway/src/main/java/com/shinhan/pg/aigateway/adapter/in/web/AiInferenceController.java) |
| MVP3-F02 | ai-gateway | GET /internal/v1/ai/requests/{requestId} | Deferred, no mapping | [AiAuditController](../../ai-vpc/ai-gateway/src/main/java/com/shinhan/pg/aigateway/adapter/in/web/AiAuditController.java) |
| MVP4-F01 | wealth-report | POST /api/v1/wealth/reports | Draft, dev-only 501 | [WealthReportController](../../main-vpc/info/wealth-report/src/main/java/com/shinhan/pg/wealthreport/adapter/in/web/WealthReportController.java) |
| MVP4-F02 | wealth-report | GET /api/v1/wealth/reports/{reportId} | Draft, dev-only 501 | [WealthReportController](../../main-vpc/info/wealth-report/src/main/java/com/shinhan/pg/wealthreport/adapter/in/web/WealthReportController.java) |
| MVP4-F03 | wealth-report | GET /api/v1/wealth/reports | Draft, dev-only 501 | [WealthReportController](../../main-vpc/info/wealth-report/src/main/java/com/shinhan/pg/wealthreport/adapter/in/web/WealthReportController.java) |
| MVP4-F04 | wealth-report | POST /internal/v1/wealth/reports/{reportId}/retry | Deferred, no mapping | [ReportOperationsController](../../main-vpc/info/wealth-report/src/main/java/com/shinhan/pg/wealthreport/adapter/in/web/ReportOperationsController.java) |
| MVP5-F01 | transfer | POST /api/v1/transfers/overseas | Draft, dev-only 501 | [OverseasTransferController](../../main-vpc/core/transfer/src/main/java/com/shinhan/pg/transfer/adapter/in/web/OverseasTransferController.java) |
| MVP5-F02 | transfer | GET /api/v1/transfers/overseas/{transferId} | Draft, dev-only 501 | [OverseasTransferController](../../main-vpc/core/transfer/src/main/java/com/shinhan/pg/transfer/adapter/in/web/OverseasTransferController.java) |
| MVP5-F04 | fep-adapter | POST /internal/v1/fep/swift/messages | Draft, dev-only 501 | [FepSwiftController](../../fep-vpc/fep-adapter/src/main/java/com/shinhan/pg/fepadapter/adapter/in/web/FepSwiftController.java) |
| MVP6-F02 | aml | GET /internal/v1/aml/cases | Draft, dev-only 501 | [AmlCaseController](../../main-vpc/info/aml/src/main/java/com/shinhan/pg/aml/adapter/in/web/AmlCaseController.java) |
| MVP6-F03 | aml | POST /internal/v1/aml/cases/{caseId}/reports | Draft, dev-only 501 | [AmlReportController](../../main-vpc/info/aml/src/main/java/com/shinhan/pg/aml/adapter/in/web/AmlReportController.java) |
| MVP7-F03 | fep-adapter | POST /internal/v1/fep/settlement/files | Draft, dev-only 501 | [FepSettlementController](../../fep-vpc/fep-adapter/src/main/java/com/shinhan/pg/fepadapter/adapter/in/web/FepSettlementController.java) |
| MVP7-F04 | settlement | GET /internal/v1/settlements/{date} | Draft, dev-only 501 | [SettlementController](../../main-vpc/core/settlement/src/main/java/com/shinhan/pg/settlement/adapter/in/web/SettlementController.java) |
| MVP7-F04 | settlement | POST /internal/v1/settlements/{date}/rerun | Draft, dev-only 501 | [SettlementController](../../main-vpc/core/settlement/src/main/java/com/shinhan/pg/settlement/adapter/in/web/SettlementController.java) |
| MVP3-F04 | ai-gateway | GET /internal/v1/ai/security-statistics | Deferred, no mapping | [AiStatisticsController](../../ai-vpc/ai-gateway/src/main/java/com/shinhan/pg/aigateway/adapter/in/web/AiStatisticsController.java) |
| MVP5-F07 | swift | GET /internal/v1/swift/transfers/{uetr}/messages | Deferred, no mapping | [SwiftOperationsController](../../main-vpc/external/swift/src/main/java/com/shinhan/pg/swift/adapter/in/web/SwiftOperationsController.java) |

## Non-HTTP Entry Points

- SWIFT: consume transfer.outbound, retransmit with the same UETR, and record the fep.ack intent after evaluating pacs.002.
- Ledger: besides internal journal posting/lookup, confirmation and reversal posting based on fep.ack.
- Transfer: apply completion from ledger.posted, and re-query UNKNOWN results.
- Payment: apply verified PG results, and recover UNKNOWN ones.
- Wealth Report: update snapshots on completed transactions, and recover interrupted synchronous generation jobs.
- AML: rule-based detection on transaction events, automatic report decisions, and retries of report submission.
- Settlement: closing, aggregation of fixed inputs, file generation/exchange, reconciliation, ledger posting and resumption.

No KafkaListener or batch schedule is registered. The TODOs in those files say to confirm the topic, payload, ack/retry, duplicate effects and cron/range. Candidate comments for the FEP PG/KoFIU/result callbacks, Gateway routing and Mock institution paths are in separately named files. Actuator health/prometheus come from the existing configuration, and no duplicate Controller is created.

## Undecided Items and Conflict Handling

- Notion's older labels (7 services, PostgreSQL only, asynchronous report acceptance) are replaced by the latest Figma/user decisions.
- Storing the original text of blocked AI requests is not confirmed. The default relay audit excludes the original text, and audit lookup/quotas/statistics are deferred.
- 401/403/409/422/429 and external-failure responses are contract candidates for the business implementation. This does not mean the current tests have verified those financial decisions.
- Amount units, required fields, pagination, service authentication, callback delivery and signature headers get a final cross-check before implementation. The identity attribute in the Controller is a placeholder for internal wiring.
- Re-querying the ledger result for the same transactionId is different from a 409 for a user's duplicate transfer. A 404 on a transfer recovery lookup alone does not declare a final failure.
