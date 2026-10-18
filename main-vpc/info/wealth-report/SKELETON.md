# wealth-report internal implementation files

DB: PostgreSQL. To implement, fill in the per-method TODOs in application/service/WealthReportApplicationService.java. The flow is HTTP Controller → input port → ApplicationService → domain/output port → persistence/client.

## API files

| Feature ID | Method and path | adapter/in/web file | Status |
| --- | --- | --- | --- |
| MVP4-F01 | POST /api/v1/wealth/reports | WealthReportController.java | Dev-only 501 |
| MVP4-F02 | GET /api/v1/wealth/reports/{reportId} | WealthReportController.java | Dev-only 501 |
| MVP4-F03 | GET /api/v1/wealth/reports | WealthReportController.java | Dev-only 501 |
| MVP4-F04 | POST /internal/v1/wealth/reports/{reportId}/retry | ReportOperationsController.java | Deferred, comments only |

## Domain and repository files

- domain/model: AssetReport, AssetSnapshot, TokenMapping
- application/port/out Repository: AssetReportRepository, AssetSnapshotRepository, TokenMappingRepository
- domain/policy: FinancialMetricsCalculator, ReportNumberValidationPolicy, SnapshotConsistencyPolicy
- adapter/out/persistence: a PersistenceAdapter, entity/*Entity and mapper/*EntityMapper per model.
- Entity ORM annotations, physical columns/relations and unique keys are left as undecided comments. No business migration has been added.

## Feature entry points

- GenerateReportUseCase
- UpdateReportSnapshotUseCase
- GetReportUseCase
- ListReportsUseCase
- RecoverReportGenerationUseCase

The default/local/aws profiles have no skeleton business beans or Controllers. Valid dev-only HTTP requests return 501, and malformed requests return 400. Real authentication, domain validation, DB/external clients, error contracts and financial concurrency are TODO. Deferred APIs keep their annotations as comments and do not register Spring beans.

For all paths and contracts, see the project's docs/prd/API_SKELETON_INDEX.md. For the basis of each feature, see FEATURE_FILE_CHECKLIST.md.
