# Remaining Implementation and Deployment Inputs

The six design choices are recorded in [Confirmed decisions and checklist](../adr/0001-financial-integration-decisions.md). Below, undecided design items are kept separate from items awaiting implementation.

1. **AI VPC → external LLM — structure in place, client implementation pending**: Confirmed as an AI-only NAT egress. The AI-only NAT, routing, external HTTPS destination CIDR restrictions and hostname check structure are in place. The NAT is disabled by default, and the destination CIDRs/hostnames are empty by default. Remaining work: wire hostname checks, redirect blocking and timeouts into the real LLM client, and set the provider destinations. Data subnet isolation stays.
2. **AWS Redis IAM**: IAM authentication and TLS were chosen so that secrets are not pulled into Terraform state. The application's IAM token creation/refresh provider and key prefix policy are not implemented. The default `redis_iam_client_ready=false` blocks incomplete ECS deployments. The local Redis connection was verified.
3. **AWS inputs**: Required: an existing S3 state bucket, per-service Secrets Manager names (username/password), immutable service image URIs, and an ACM ARN if the public gateway is enabled. Service DB accounts and permissions are not prepared. Terraform reads only secret metadata; the code receives the values by injection in the deployed ECS.
4. **AWS observability destination**: The local Collector, Prometheus and Grafana were verified. The deployment location of the AWS Collector/trace store and the private collection address are undecided. An OTLP collection address must be provided at deployment.

The MariaDB + PostgreSQL choice was resolved by the user's confirmation. account and ledger use MariaDB, and the other 9 services that use a data store use PostgreSQL. The business tables from the old ERD were not copied into this empty Flyway baseline.

JWT RS256 issuance/verification/revocation, OTP, rate limiting, the business Outbox storage adapter, API and event contracts, amount calculation and exchange with external institutions belong to the follow-up business implementation phase. Until the verification TODOs are done, the Gateway blocks all requests other than health by default.

References: [VPC peering limitations](https://docs.aws.amazon.com/vpc/latest/peering/vpc-peering-basics.html), [ElastiCache IAM](https://docs.aws.amazon.com/AmazonElastiCache/latest/dg/auth-iam.html).

For the synchronous report response, the time budget and the concurrent generation count exist only as settings with value validation. Actual limit enforcement, financial business entities, persistence SQL, Outbox atomicity, and locking and recovery guarantees are follow-up work. Current verification covers only structure and technical settings, and follows the [final skeleton guide](SKELETON_GUIDE.md).

Per-service API/DTO, domain and persistence files and implementation TODOs are linked from the [API list](API_SKELETON_INDEX.md) and the [per-feature file checklist](FEATURE_FILE_CHECKLIST.md). The 501/400 checks on the 36 dev-only paths and the absence of mappings for the 5 deferred APIs were verified. Successful financial responses, authentication, the final HTTP contract and real ORM/SQL are follow-up work.
