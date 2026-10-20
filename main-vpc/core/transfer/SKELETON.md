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

## MVP5 해외송금 (2026-10-20)

USD/no-FX/zero-fee 개발 계약을 `OverseasTransferService`와 별도 PostgreSQL V4 aggregate로 구현했다. 기존 국내 이체 완료 소비와 분리해 대기 분개만으로 이체가 완료되지 않는다. 인증·OTP·소유권·멱등성, 원장 예약, Outbox, Ledger 수렴, 관리자 원문/일반 본인 마스킹 조회와 수동 결과조회/재전송을 구현한다. 48시간 후 개인정보는 삭제하고 식별자·최종 결과를 유지한다. Account의 USD 계좌 개설은 명시한 개발 게이트에서만 추가된다. 기본 재전송 false, 운영 미활성. [전체 API·옵션·검증·Figma 차이](../../external/swift/SKELETON.md)를 따른다.
