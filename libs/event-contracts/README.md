# Event contracts
Package: `com.shinhan.pg.common.eventcontracts`.
Only immutable Kafka event shapes belong here. No producers, consumers, clients or business rules.
Versioned contracts are additive: do not remove or rename V1 fields. Breaking changes require V2.
No topics or business event types are defined in this scaffold.
