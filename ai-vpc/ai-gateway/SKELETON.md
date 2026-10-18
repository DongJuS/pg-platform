# ai-gateway internal implementation files

DB: PostgreSQL. To implement, fill in the per-method TODOs in application/service/AiGatewayApplicationService.java. The flow is HTTP Controller → input port → ApplicationService → domain/output port → persistence/client.

## API files

| Feature ID | Method and path | adapter/in/web file | Status |
| --- | --- | --- | --- |
| MVP3-F01 | POST /internal/v1/ai/inference | AiInferenceController.java | Deferred, comments only |
| MVP3-F02 | GET /internal/v1/ai/requests/{requestId} | AiAuditController.java | Deferred, comments only |
| MVP3-F04 | GET /internal/v1/ai/security-statistics | AiStatisticsController.java | Deferred, comments only |

## Domain and repository files

- domain/model: AiRequestAudit
- application/port/out Repository: AiRequestAuditRepository
- domain/policy: AiRequestInspectionPolicy
- adapter/out/persistence: a PersistenceAdapter, entity/*Entity and mapper/*EntityMapper per model.
- Entity ORM annotations, physical columns/relations and unique keys are left as undecided comments. No business migration has been added.

## Feature entry points

- RelayLlmUseCase

The default/local/aws profiles have no skeleton business beans or Controllers. Valid dev-only HTTP requests return 501, and malformed requests return 400. Real authentication, domain validation, DB/external clients, error contracts and financial concurrency are TODO. Deferred APIs keep their annotations as comments and do not register Spring beans.

The AI relay role and the NAT egress are confirmed. The inference HTTP path and the detailed security contract stay marked as deferred.

For all paths and contracts, see the project's docs/prd/API_SKELETON_INDEX.md. For the basis of each feature, see FEATURE_FILE_CHECKLIST.md.
