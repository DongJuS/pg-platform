---
status: accepted
date: 2026-10-18
---

# Implementation Baseline for Financial Service Communication and Consistency

After reviewing the system architecture diagram in the latest Figma, the user chose 1-A, 2-B, 3-A, 4-A, 5-A and 6-A. The design below is confirmed; it does not mean the implementation is complete. It replaces the earlier guidance to send all FEP requests over Kafka and to place a NAT only in FEP.

| No. | Confirmed decision | Impact of the choice |
| --- | --- | --- |
| 1-A | Financial external requests for SWIFT, PG, AML and settlement all use internal REST from the caller → FEP. | Keep the acceptance response separate from the external institution's final result. Kafka propagation of completion events stays. Timeouts, re-queries and retransmissions use the same business identifier. |
| 2-B | Ledger publishes `ledger.posted`, and Transfer publishes `transfer.completed` after it confirms the transfer status. | The ledger owns journal posting, and the transfer service owns transfer completion. Each service wraps its state change and Outbox write in its own local DB transaction. |
| 3-A | Set up a NAT egress in the AI VPC that is independent of FEP. | The existing FEP NAT stays for financial external traffic. AI's allowed destinations are restricted by separate controls, and the data subnet gets no default route to the internet. Recalculate the cost of the added NAT. |
| 4-A | The report API waits until AI draft validation, PDF generation and storage finish, then responds with the completed result. | A success response includes access information for the generated report. Separately from the synchronous API response, store the processing stage, as-of time and recovery state. The Kafka snapshot update is a separate feature. |
| 5-A | Outbox starts with DB polling. | Keep the existing relay direction. Assume at-least-once delivery, which allows duplicate delivery: implement duplicate prevention in consumers, and tune batch size, interval and concurrency through load measurement. Adopting CDC is not part of this decision. |
| 6-A | Ledger controls concurrent withdrawals from the same account with pessimistic row locks. | Lock in a consistent account ID order, and run the balance check, journal posting, balance update and Outbox write in a short local transaction. Define a bounded retry policy for deadlocks and lock waits. |

MariaDB (account, ledger) + PostgreSQL (the rest) and the existing deployment order stay. Do not assume that both DBs and all HTTP calls are wrapped in one plain `@Transactional`. If the response is lost after the ledger posting is applied, check the result with the existing transaction ID. Do not immediately mark the Transfer as failed, and do not repeat the withdrawal as a new transaction. Make status confirmation and event recording idempotent so that the REST response and `ledger.posted` consumption do not run the same completion handling twice.

A synchronous report response does not mean keeping the HTTP connection open indefinitely. Define the wait budgets for the LLM, PDF and storage stages and for the Gateway/server, together with a limit on concurrent generations. Do not respond to a timeout as completion, and do not automatically switch to a 202 job-accepted contract. HTTP statuses, required fields and event payloads are follow-up contract work.

The final design's use case, port, adapter and profile wiring is reflected in the Java skeleton, and the R7, internal FEP SG and independent AI NAT infrastructure code is also in place. Financial business logic bodies, persistence SQL and real external clients are not implemented, and no AWS resources were created. Below, structural changes and real behavior verification are listed separately.

## Implementation Checklist

- [x] Record the six choices as accepted decisions and link them from the project entry documents.
- [x] Correct the current guidance that said all FEP requests go over Kafka and that AI egress is undecided.
- [x] Structure the 53 use cases and the API/internal files of the 11 general services by HTTP, Kafka and batch role.
- [x] Add the caller-side adapter/out/fep for SWIFT, Payment, AML and Settlement, and the FEP-internal locations for acceptance, result receipt and result lookup.
- [x] Pass an R7 check that allows the internal FEP REST client boundary and rejects direct clients to external institutions.
- [x] Cross-check the main → FEP internal REST 8091 SG against the existing direct peering path.
- [x] Separate the ledger/transfer event responsibilities from the atomic local write ports, and distinguish trace ID, idempotency key, transaction ID and eventId.
- [x] Add code for an AI-only NAT, app-only routing and destination CIDR restrictions, keeping them disabled by default and blocked by default.
- [x] Create the location for exact LLM HTTPS hostname checks and an allowlist that is empty by default.
- [x] Update the development environment cost range estimate to include two NATs.
- [x] Split synchronous report generation from the separate snapshot update, and add value validation for the time and concurrency settings.
- [x] Separate the batch/lease/ack time settings for Outbox polling.
- [x] After expanding the API and internal files, the build passes 69 tests. The earlier infrastructure checks (Terraform fmt/validate, 6 mocks) and the CI wiring are kept.
- [ ] Implement the wire contract for financial external REST (service authentication, HTTP/event fields, callback/result propagation) and the real clients.
- [ ] Implement real DB atomicity for the ledger/transfer local Outbox writes and for duplicate-completion prevention.
- [ ] Verify recovery from a lost response after ledger success, from duplicate events and from service restarts.
- [ ] Wire hostname checks, redirect blocking and timeouts into the LLM client, and configure the real provider destination and authentication.
- [ ] Enforce the per-stage report limits and the concurrent generation limit, and tie them to the Gateway time budget.
- [ ] Measure Outbox polling at the target load, and tune the unpublished backlog, duplicates and delivery latency.
- [ ] Implement and verify MariaDB lock SQL, duplicate prevention for a single transaction, concurrent withdrawals and deadlock recovery.
- [ ] Update the Ledger transfer.completed label in Figma to match the publisher in this decision, and cross-check the other labels.

## Review Sources

- [Figma system architecture diagram](https://www.figma.com/design/JOeniY2Om7ofAt39NluJXl/sinhan-financial-project?node-id=19-2)
- [Transfer → Ledger synchronous REST and the existing ledger event labels](https://www.figma.com/design/JOeniY2Om7ofAt39NluJXl/sinhan-financial-project?node-id=24-42)
- [SWIFT's REST transmission to FEP](https://www.figma.com/design/JOeniY2Om7ofAt39NluJXl/sinhan-financial-project?node-id=32-14)
- [AI NAT egress label](https://www.figma.com/design/JOeniY2Om7ofAt39NluJXl/sinhan-financial-project?node-id=26-41)
- [Report generation and snapshot consumption](https://www.figma.com/design/JOeniY2Om7ofAt39NluJXl/sinhan-financial-project?node-id=29-2)

The publisher shown in Figma at the time of review differs from 2-B above. The details of the synchronous report response, polling and locking were confirmed by the user's choice. Differences between this record and Figma are tracked as not-yet-applied work.
