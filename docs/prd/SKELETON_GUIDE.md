# Service Internal Implementation Skeleton

This skeleton consists of each service's real per-feature files, methods, input/output DTOs and processing TODOs. It expands the earlier representative use case frame into 53 use cases and 41 API definitions across 11 general services. Of the API definitions, 36 are mapped only in the skeleton profile, and 5 are deferred with comments only. Routing, filter, institution API and scenario files for the Gateway and the 4 Mocks are also provided separately.

## Files to Start With

- [AuthController — sign-up, login, refresh, logout, OTP](../../main-vpc/core/auth/src/main/java/com/shinhan/pg/auth/adapter/in/web/AuthController.java)
- [TransferController — transfer creation, status, list](../../main-vpc/core/transfer/src/main/java/com/shinhan/pg/transfer/adapter/in/web/TransferController.java)
- [Full API list](API_SKELETON_INDEX.md)
- [Feature ID → file checklist](FEATURE_FILE_CHECKLIST.md)

Each API method comment states the feature ID, HTTP method/path, contract status, authentication assumption, input/output, success and error candidates, undecided items and source. Each ApplicationService method has the processing order to fill in written as a TODO. This completes the contract and file work; it does not mean the financial features are complete.

## Scope by Service

| Service | Dev-only HTTP paths | Deferred APIs | Wired use cases | File guide |
| --- | ---: | ---: | ---: | --- |
| auth | 6 | 0 | 6 | [SKELETON.md](../../main-vpc/core/auth/SKELETON.md) |
| account | 4 | 0 | 4 | [SKELETON.md](../../main-vpc/core/account/SKELETON.md) |
| ledger | 4 | 0 | 5 | [SKELETON.md](../../main-vpc/core/ledger/SKELETON.md) |
| transfer | 5 | 0 | 7 | [SKELETON.md](../../main-vpc/core/transfer/SKELETON.md) |
| payment | 8 | 0 | 10 | [SKELETON.md](../../main-vpc/core/payment/SKELETON.md) |
| settlement | 2 | 0 | 3 | [SKELETON.md](../../main-vpc/core/settlement/SKELETON.md) |
| swift | 0 | 1 | 3 | [SKELETON.md](../../main-vpc/external/swift/SKELETON.md) |
| wealth-report | 3 | 1 | 5 | [SKELETON.md](../../main-vpc/info/wealth-report/SKELETON.md) |
| aml | 2 | 0 | 4 | [SKELETON.md](../../main-vpc/info/aml/SKELETON.md) |
| ai-gateway | 0 | 3 | 1 | [SKELETON.md](../../ai-vpc/ai-gateway/SKELETON.md) |
| fep-adapter | 2 | 0 | 5 | [SKELETON.md](../../fep-vpc/fep-adapter/SKELETON.md) |

## Where to Fill In Files

```text
<service>/src/main/java/com/shinhan/pg/<service>/
├─ adapter/in/web/
│  ├─ <Business>Controller.java  # HTTP path, identity, request conversion, response candidates
│  ├─ request/<Feature>Request.java
│  ├─ response/<Feature>Response.java
│  └─ <Service>ErrorMapper.java  # business error → HTTP contract, currently TODO
├─ application/
│  ├─ command/<Feature>Command.java
│  ├─ result/<Feature>Result.java
│  ├─ port/in/<Feature>UseCase.java
│  ├─ port/out/<Model>Repository.java
│  └─ service/<Service>ApplicationService.java
├─ domain/
│  ├─ model/<BusinessModel>.java  # field and invariant comments, pure Java
│  ├─ policy/<BusinessRule>.java  # validation/calculation methods and implementation TODOs
│  └─ error/<Service>ErrorCode.java
├─ adapter/out/persistence/
│  ├─ <Model>PersistenceAdapter.java
│  ├─ entity/<Model>Entity.java  # field placeholders, ORM/column mapping undecided
│  ├─ mapper/<Model>EntityMapper.java
│  └─ outbox/<Service>OutboxStore.java
└─ config/<Service>SkeletonConfiguration.java
```

Read models for queries are kept separate from Entities. Business model, Repository, Entity and Mapper files were created for User/Account/Journal/Posting/Transfer/Payment/Refund/Report/SwiftMessage/AmlCase/FepMessage/SettlementCycle and others. JPA/JdbcClient/MyBatis and the physical columns were not arbitrarily fixed, and no business migrations were added. The real data contract is confirmed while filling in the unimplemented methods of each Repository/Mapper.

## Decisions Kept

- [Confirmed ADR](../adr/0001-financial-integration-decisions.md): financial external requests use internal REST → FEP, and publishing responsibility is split between the ledger (ledger.posted) and the transfer service (transfer.completed).
- Reports respond synchronously after the PDF/S3 save completes. The 202 acceptance and asynchronous queue from older documents were not copied over as confirmed. The generation stage, as-of time and recovery jobs stay.
- The implementation locations and transaction conditions for Outbox polling and the ledger's pessimistic locking are written as comments. Real SQL, concurrency and DB atomicity are not implemented yet.
- account/ledger use MariaDB, and the other storage services use PostgreSQL. The existing AI-only NAT code and the deployment order stay. This work did not call or deploy to AWS.
- Deferred features such as extended logout, AI quotas/statistics and SWIFT operations lookup have only code comments/file placeholders, and no mappings are registered. The AI relay role is confirmed, but the detailed inference HTTP contract keeps its existing deferred label.

## Runtime Status and Verification

The default/local/aws profiles do not register the new business Controllers or ApplicationServices. When a valid request is called in the skeleton profile, the shared web handler converts FeatureNotImplementedException to **501**. Format errors return **400**. Comments and annotations such as 200/201/202/204 are drafts of the success contract to implement, and they do not currently produce any financial success.

Authentication, authorization, Bean Validation, HTTP success/failure details, pagination, schemas, ledger logic, external clients and real ORM/SQL are TODO. The request attribute is a placeholder for wiring a trusted authentication context; do not treat it as real JWT/service authentication. The API contract tests verify the format and not-implemented markers at this skeleton stage. When implementing a feature, replace those checks with real acceptance-criteria tests.

The full build has **69 tests passing**, with 0 failures/errors/skips. Verified: the 36 dev-only paths, the 5 deferred APIs not exposed, the default profile inactive, HTTP 501/400, wiring of 53 use cases, R1~R7, and the existing PostgreSQL/Kafka/Redis Testcontainers checks. This is not verification of financial consistency, real concurrent withdrawals or target throughput.

### 2026-10-18 Skeleton Gap Check

- Cross-checked the 77 feature IDs against the source. Only the gateway's CMN-F02, MVP2-F10 and MVP2-F11 were missing from source comments, so the feature IDs, implementation scope and undecided items were added to GatewayRoutesConfiguration, GatewayJwtFilter, GatewayRateLimitFilter and InternalPathBlockFilter (no change to routing behavior). The other 74 are traceable in the source.
- In transfer, `domain/TransferErrorCode` (an example implementation of the shared ErrorCode), a leftover from the first scaffold, duplicated `domain/error/TransferErrorCode` (the business error draft) and had no references, so it was deleted.
- Re-verified that all 147 files referenced by the checklist and all 544 in api-skeleton-files.json exist, that the 5 deferred mappings are inactive, and that 36 mappings are active in the skeleton profile.
- Figma frames could not be checked because they cannot be accessed without authentication. Figma-dependent items such as the gateway health and metrics scope carry a TODO in the source comments saying the Figma frame still needs checking. Consistency between the confirmed ADR decisions and the code/docs was verified locally.

[Verification results](verification-api-skeleton.json) · [Build log](verification-api-skeleton-build.log) · [File list](api-skeleton-files.json)
