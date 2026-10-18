# aml internal implementation files

DB: PostgreSQL. To implement, fill in the per-method TODOs in application/service/AmlApplicationService.java. The flow is HTTP Controller → input port → ApplicationService → domain/output port → persistence/client.

## API files

| Feature ID | Method and path | adapter/in/web file | Status |
| --- | --- | --- | --- |
| MVP6-F02 | GET /internal/v1/aml/cases | AmlCaseController.java | Dev-only 501 |
| MVP6-F03 | POST /internal/v1/aml/cases/{caseId}/reports | AmlReportController.java | Dev-only 501 |

## Domain and repository files

- domain/model: AmlCase, AmlReport, DetectionRule
- application/port/out Repository: AmlCaseRepository, AmlReportRepository, DetectionRuleRepository
- domain/policy: AmlReportingDecisionPolicy, AmlDeduplicationPolicy
- adapter/out/persistence: a PersistenceAdapter, entity/*Entity and mapper/*EntityMapper per model.
- Entity ORM annotations, physical columns/relations and unique keys are left as undecided comments. No business migration has been added.

## Feature entry points

- EvaluateTransactionUseCase
- RetryAmlReportUseCase
- ListAmlCasesUseCase
- SubmitAmlReportUseCase

The default/local/aws profiles have no skeleton business beans or Controllers. Valid dev-only HTTP requests return 501, and malformed requests return 400. Real authentication, domain validation, DB/external clients, error contracts and financial concurrency are TODO. Deferred APIs keep their annotations as comments and do not register Spring beans.

For all paths and contracts, see the project's docs/prd/API_SKELETON_INDEX.md. For the basis of each feature, see FEATURE_FILE_CHECKLIST.md.
