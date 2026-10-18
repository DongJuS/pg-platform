# swift internal implementation files

DB: PostgreSQL. To implement, fill in the per-method TODOs in application/service/SwiftApplicationService.java. The flow is HTTP Controller → input port → ApplicationService → domain/output port → persistence/client.

## API files

| Feature ID | Method and path | adapter/in/web file | Status |
| --- | --- | --- | --- |
| MVP5-F07 | GET /internal/v1/swift/transfers/{uetr}/messages | SwiftOperationsController.java | Deferred, comments only |

## Domain and repository files

- domain/model: SwiftMessage
- application/port/out Repository: SwiftMessageRepository
- domain/policy: Mt103Parser, SwiftFinalityPolicy
- adapter/out/persistence: a PersistenceAdapter, entity/*Entity and mapper/*EntityMapper per model.
- Entity ORM annotations, physical columns/relations and unique keys are left as undecided comments. No business migration has been added.

## Feature entry points

- SubmitSwiftUseCase
- RetrySwiftUseCase
- RecordSwiftAcknowledgementUseCase

The default/local/aws profiles have no skeleton business beans or Controllers. Valid dev-only HTTP requests return 501, and malformed requests return 400. Real authentication, domain validation, DB/external clients, error contracts and financial concurrency are TODO. Deferred APIs keep their annotations as comments and do not register Spring beans.

For all paths and contracts, see the project's docs/prd/API_SKELETON_INDEX.md. For the basis of each feature, see FEATURE_FILE_CHECKLIST.md.
