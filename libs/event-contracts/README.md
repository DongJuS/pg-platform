# Event contracts

Package: `com.shinhan.pg.common.eventcontracts`. Immutable versioned Kafka event shapes only; no producers, consumers, HTTP clients or DB access. V1 fields are additive; breaking changes require V2.

| Topic | Actual payload | Publisher → consumer |
| --- | --- | --- |
| ledger.posted | LedgerPostedEvent | Ledger → Transfer / report projection |
| transfer.completed | TransferCompletedEvent | Transfer → report projection |
| transfer.outbound | TransferOutboundEvent | Transfer after confirmed hold → Swift |
| fep.ack | SwiftAcknowledgedEvent | Swift after validated/correlated result → Ledger |
| ledger.overseas-applied | OverseasLedgerAppliedEvent | Ledger after final posting/review → Transfer |

Event IDs deduplicate deliveries; transfer/UETR/transaction IDs identify the financial business/effect. Receipt, institution outcome and Ledger completion are distinct. Overseas hold journals never complete a domestic transfer. Private remittance fields are fetched through authenticated source-owned REST, never placed in these event payloads.

Actual persistence, Outbox/inbox contracts, activation gates, consumers and recovery limits are in [MVP5 checklist](../../main-vpc/external/swift/SKELETON.md). Creating a topic does not create a wire contract or consumer.

## MVP6 AML consumer

AML consumes the existing `ledger.posted` and `transfer.completed` V1 bytes with a fresh-group `earliest` policy. It commits a durable local Inbox before acknowledging Kafka and fetches source-owned evidence through dedicated authenticated REST. It does not infer final Ledger effects from `fep.ack`, aggregate overseas holds as remittances, or infer amount/customer from event IDs. Canonical Ledger transaction identity deduplicates financial facts across topics. `aml.report-ready` Outbox notifications contain only stable report/case IDs and rule version; local durable report work is recovered independently of notification delivery. See the AML checklist for policy, expiry and recovery.
