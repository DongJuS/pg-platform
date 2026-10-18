# auth internal implementation files

DB: PostgreSQL. To implement, fill in the per-method TODOs in application/service/AuthApplicationService.java. The flow is HTTP Controller → input port → ApplicationService → domain/output port → persistence/client.

## API files

| Feature ID | Method and path | adapter/in/web file | Status |
| --- | --- | --- | --- |
| MVP1-F01 | POST /api/v1/auth/register | AuthController.java | Dev-only 501 |
| MVP1-F02 | POST /api/v1/auth/login | AuthController.java | Dev-only 501 |
| MVP1-F03 | POST /api/v1/auth/refresh | AuthController.java | Dev-only 501 |
| MVP1-F04 | POST /api/v1/auth/logout | AuthController.java | Dev-only 501 |
| MVP1-F05 | GET /api/v1/users/me | UserProfileController.java | Dev-only 501 |
| MVP1-F06 | POST /api/v1/auth/otp/verify | AuthController.java | Dev-only 501 |

## Domain and repository files

- domain/model: User, RefreshSession, OtpProof
- application/port/out Repository: UserRepository, RefreshSessionRepository, OtpProofRepository
- domain/policy: PasswordPolicy, RefreshRotationPolicy, OtpVerificationPolicy
- adapter/out/persistence: a PersistenceAdapter, entity/*Entity and mapper/*EntityMapper per model.
- Entity ORM annotations, physical columns/relations and unique keys are left as undecided comments. No business migration has been added.

## Feature entry points

- AuthenticateUseCase
- RegisterUserUseCase
- RefreshTokenUseCase
- LogoutUseCase
- GetCurrentUserUseCase
- VerifyOtpUseCase

The default/local/aws profiles have no skeleton business beans or Controllers. Valid dev-only HTTP requests return 501, and malformed requests return 400. Real authentication, domain validation, DB/external clients, error contracts and financial concurrency are TODO. Deferred APIs keep their annotations as comments and do not register Spring beans.

For all paths and contracts, see the project's docs/prd/API_SKELETON_INDEX.md. For the basis of each feature, see FEATURE_FILE_CHECKLIST.md.
