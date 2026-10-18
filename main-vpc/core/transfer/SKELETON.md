# transfer internal implementation files

DB: PostgreSQL. To implement, fill in the per-method TODOs in application/service/TransferApplicationService.java. The flow is HTTP Controller → input port → ApplicationService → domain/output port → persistence/client.

## API files

| Feature ID | Method and path | adapter/in/web file | Status |
| --- | --- | --- | --- |
| MVP1-F15 | POST /api/v1/transfers | TransferController.java | Dev-only 501 |
| MVP1-F16 | GET /api/v1/transfers/{transferId} | TransferController.java | Dev-only 501 |
| MVP1-F17 | GET /api/v1/transfers | TransferController.java | Dev-only 501 |
| MVP5-F01 | POST /api/v1/transfers/overseas | OverseasTransferController.java | Dev-only 501 |
| MVP5-F02 | GET /api/v1/transfers/overseas/{transferId} | OverseasTransferController.java | Dev-only 501 |

## Domain and repository files

- domain/model: Transfer, TransferHistory, OverseasTransferDetails
- application/port/out Repository: TransferRepository, TransferHistoryRepository, OverseasTransferDetailsRepository
- domain/policy: TransferIdempotencyPolicy, TransferStatePolicy, OverseasTransferConvergencePolicy
- adapter/out/persistence: a PersistenceAdapter, entity/*Entity and mapper/*EntityMapper per model.
- Entity ORM annotations, physical columns/relations and unique keys are left as undecided comments. No business migration has been added.

## Feature entry points

- RequestTransferUseCase
- CompleteTransferUseCase
- RecoverTransferUseCase
- GetTransferUseCase
- ListTransfersUseCase
- RequestOverseasTransferUseCase
- GetOverseasTransferUseCase

The default/local/aws profiles have no skeleton business beans or Controllers. Valid dev-only HTTP requests return 501, and malformed requests return 400. Real authentication, domain validation, DB/external clients, error contracts and financial concurrency are TODO. Deferred APIs keep their annotations as comments and do not register Spring beans.

For all paths and contracts, see the project's docs/prd/API_SKELETON_INDEX.md. For the basis of each feature, see FEATURE_FILE_CHECKLIST.md.
