# payment internal implementation files

DB: PostgreSQL. To implement, fill in the per-method TODOs in application/service/PaymentApplicationService.java. The flow is HTTP Controller → input port → ApplicationService → domain/output port → persistence/client.

## API files

| Feature ID | Method and path | adapter/in/web file | Status |
| --- | --- | --- | --- |
| MVP2-F01 | POST /api/v1/payments | PaymentController.java | Dev-only 501 |
| MVP2-F02 | GET /api/v1/payments/{paymentId} | PaymentController.java | Dev-only 501 |
| MVP2-F03 | POST /api/v1/payments/{paymentId}/cancel | PaymentController.java | Dev-only 501 |
| MVP2-F04 | POST /api/v1/payments/{paymentId}/refunds | PaymentController.java | Dev-only 501 |
| MVP2-F05 | GET /api/v1/payments/{paymentId}/refunds | PaymentController.java | Dev-only 501 |
| MVP2-F06 | POST /mock/v1/pg/webhooks | PaymentWebhookController.java | Dev-only 501 |
| MVP2-F08 | POST /internal/v1/payments/reconciliation-runs | PaymentReconciliationController.java | Dev-only 501 |
| MVP2-F09 | GET /internal/v1/payments/reconciliation-runs/{runId} | PaymentReconciliationController.java | Dev-only 501 |

## Domain and repository files

- domain/model: Payment, Refund, PgEvent, ReconciliationRun
- application/port/out Repository: PaymentRepository, RefundRepository, PgEventRepository, ReconciliationRunRepository
- domain/policy: RefundLimitPolicy, PaymentStatePolicy, PgCircuitBreakerPolicy
- adapter/out/persistence: a PersistenceAdapter, entity/*Entity and mapper/*EntityMapper per model.
- Entity ORM annotations, physical columns/relations and unique keys are left as undecided comments. No business migration has been added.

## Feature entry points

- ApprovePaymentUseCase
- ApplyPaymentConfirmationUseCase
- GetPaymentUseCase
- CancelPaymentUseCase
- RequestRefundUseCase
- ListRefundsUseCase
- ReceivePgWebhookUseCase
- StartReconciliationUseCase
- GetReconciliationUseCase
- RecoverPaymentUseCase

The default/local/aws profiles have no skeleton business beans or Controllers. Valid dev-only HTTP requests return 501, and malformed requests return 400. Real authentication, domain validation, DB/external clients, error contracts and financial concurrency are TODO. Deferred APIs keep their annotations as comments and do not register Spring beans.

For all paths and contracts, see the project's docs/prd/API_SKELETON_INDEX.md. For the basis of each feature, see FEATURE_FILE_CHECKLIST.md.
