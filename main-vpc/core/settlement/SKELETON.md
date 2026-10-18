# settlement internal implementation files

DB: PostgreSQL. To implement, fill in the per-method TODOs in application/service/SettlementApplicationService.java. The flow is HTTP Controller → input port → ApplicationService → domain/output port → persistence/client.

## API files

| Feature ID | Method and path | adapter/in/web file | Status |
| --- | --- | --- | --- |
| MVP7-F04 | GET /internal/v1/settlements/{date} | SettlementController.java | Dev-only 501 |
| MVP7-F04 | POST /internal/v1/settlements/{date}/rerun | SettlementController.java | Dev-only 501 |

## Domain and repository files

- domain/model: SettlementCycle, SettlementLine, SettlementFile
- application/port/out Repository: SettlementCycleRepository, SettlementLineRepository, SettlementFileRepository
- domain/policy: NettingCalculator, SettlementCutoffPolicy, SettlementRecoveryPolicy
- adapter/out/persistence: a PersistenceAdapter, entity/*Entity and mapper/*EntityMapper per model.
- Entity ORM annotations, physical columns/relations and unique keys are left as undecided comments. No business migration has been added.

## Feature entry points

- CloseSettlementUseCase
- GetSettlementUseCase
- RerunSettlementUseCase

The default/local/aws profiles have no skeleton business beans or Controllers. Valid dev-only HTTP requests return 501, and malformed requests return 400. Real authentication, domain validation, DB/external clients, error contracts and financial concurrency are TODO. Deferred APIs keep their annotations as comments and do not register Spring beans.

For all paths and contracts, see the project's docs/prd/API_SKELETON_INDEX.md. For the basis of each feature, see FEATURE_FILE_CHECKLIST.md.
