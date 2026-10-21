# AML development wire contracts

Immutable source-owned financial evidence, development STR report and separate transmission/institution result contracts. No DB, client, producer/consumer or business aggregate is shared. Each service owns its HTTP adapter, authentication and persistence.

`AmlReportDocument` carries internal IDs, currency, amount/count, rule version, score, window/reasons and amendment linkage. It never carries recipient names, account numbers, credentials or OTPs. SHA-256 covers all frozen fields in a deterministic canonical representation. Same reportKey with changed content or report identity is a conflict. `revision=1` is an original `STR_SIMULATION`; revisions after it use `STR_AMENDMENT_SIMULATION`, `supersedesReportId` and `LATE_EVIDENCE`.

`KofiuResult` separates FEP receiptReference, transmissionStatus, institutionStatus and optional AML workflowStatus. HTTP 202 means a durable FEP receipt and does not mean final institution acceptance. These are local simulation contracts, not a real KoFIU format or legal/regulatory standard.

Event V1 shapes in event-contracts stay unchanged. The Ledger transaction identity deduplicates financial facts across topics; a new rule version creates a separate evaluation, not another turnover fact.
