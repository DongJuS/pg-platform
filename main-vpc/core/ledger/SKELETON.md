# ledger internal implementation files

DB: MariaDB. To implement, fill in the per-method TODOs in application/service/LedgerApplicationService.java. The flow is HTTP Controller → input port → ApplicationService → domain/output port → persistence/client.

## API files

| Feature ID | Method and path | adapter/in/web file | Status |
| --- | --- | --- | --- |
| MVP1-F11 | GET /api/v1/accounts/{accountId}/balances | LedgerAccountController.java | Dev-only 501 |
| MVP1-F12 | GET /api/v1/accounts/{accountId}/entries | LedgerAccountController.java | Dev-only 501 |
| MVP1-F13 | POST /internal/v1/ledger/postings | InternalLedgerController.java | Dev-only 501 |
| MVP1-F14 | GET /internal/v1/ledger/transactions/{transactionId} | InternalLedgerController.java | Dev-only 501 |

## Domain and repository files

- domain/model: LedgerAccount, Journal, Posting
- application/port/out Repository: LedgerAccountRepository, JournalRepository, PostingRepository
- domain/policy: JournalBalancePolicy, BalanceProtectionPolicy
- adapter/out/persistence: a PersistenceAdapter, entity/*Entity and mapper/*EntityMapper per model.
- Entity ORM annotations, physical columns/relations and unique keys are left as undecided comments. No business migration has been added.

## Feature entry points

- PostJournalUseCase
- ApplyInstitutionResultUseCase
- QueryJournalUseCase
- GetBalanceUseCase
- ListJournalEntriesUseCase

The default/local/aws profiles have no skeleton business beans or Controllers. Valid dev-only HTTP requests return 501, and malformed requests return 400. Real authentication, domain validation, DB/external clients, error contracts and financial concurrency are TODO. Deferred APIs keep their annotations as comments and do not register Spring beans.

For all paths and contracts, see the project's docs/prd/API_SKELETON_INDEX.md. For the basis of each feature, see FEATURE_FILE_CHECKLIST.md.
